"""Build editable vanilla structure templates without adding runtime dependencies.

Run: python3 tools/build_labyrinth.py
Minecraft structure NBT, DataVersion 5023. The structures are independent test arenas:
labyrinth_trial (main encounter) and labyrinth_vault (additional reward room).
"""
from pathlib import Path
import gzip, io, struct, json
from collections import Counter

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/data/miggi_exploration/structure'
OUT.mkdir(parents=True,exist_ok=True)

class Tag:
    def __init__(self,t,v): self.t,self.v=t,v

def S(v): return Tag(8,v)
def I(v): return Tag(3,v)
def D(v): return Tag(6,v)
def C(v): return Tag(10,v)
def L(t,vs): return Tag(9,(t,vs))
def IA(v): return Tag(11,v)

def utf(x):
    x=x.encode('utf8');return struct.pack('>H',len(x))+x

def payload(t):
    typ,v=t.t,t.v
    if typ==3:return struct.pack('>i',v)
    if typ==6:return struct.pack('>d',v)
    if typ==8:return utf(v)
    if typ==11:return struct.pack('>i',len(v))+b''.join(struct.pack('>i',x) for x in v)
    if typ==9:
        ch,vs=v
        return struct.pack('>Bi',ch,len(vs))+b''.join(payload(x) for x in vs)
    if typ==10:return b''.join(bytes([child.t])+utf(name)+payload(child) for name,child in v.items())+b'\x00'
    raise ValueError(typ)

def write_nbt(path,compound):
    out=bytes([10])+utf('')+payload(C(compound))
    with gzip.open(path,'wb',compresslevel=9) as f:f.write(out)

class Structure:
    def __init__(self,name,size):
        self.name=name;self.size=size;self.blocks={};self.entities=[]
    def set(self,x,y,z,name,properties=None,nbt=None):
        if not(0<=x<self.size[0] and 0<=y<self.size[1] and 0<=z<self.size[2]):return
        self.blocks[(x,y,z)]=(name, tuple(sorted((properties or {}).items())), nbt)
    def fill(self,x1,y1,z1,x2,y2,z2,name):
        for x in range(x1,x2+1):
            for y in range(y1,y2+1):
                for z in range(z1,z2+1):self.set(x,y,z,name)
    def entity(self,x,y,z,ident):
        self.entities.append(C({'pos':L(6,[D(x),D(y),D(z)]),'blockPos':IA([int(x),int(y),int(z)]),'nbt':C({'id':S(ident)})}))
    def save(self):
        palette=[];lookup={};blocks=[]
        for (x,y,z),(name,props,nbt) in sorted(self.blocks.items()):
            key=(name,props)
            if key not in lookup:
                lookup[key]=len(palette)
                p={'Name':S(name)}
                if props:p['Properties']=C({k:S(v) for k,v in props})
                palette.append(C(p))
            b={'pos':IA([x,y,z]),'state':I(lookup[key])}
            if nbt:b['nbt']=C(nbt)
            blocks.append(C(b))
        data={'DataVersion':I(5023),'size':L(3,[I(n) for n in self.size]),'palette':L(10,palette),'blocks':L(10,blocks),'entities':L(10,self.entities)}
        write_nbt(OUT/(self.name+'.nbt'),data)
        return {'name':self.name,'size':self.size,'blocks':len(blocks),'entities':len(self.entities),'palette':len(palette)}

AIR='minecraft:air';STONE='minecraft:stone_bricks';MOSS='miggi_exploration:mossy_rune_stone';RUNE='miggi_exploration:rune_stone';CRACK='miggi_exploration:cracked_rune_stone';GLOW='miggi_exploration:glowing_rune_stone'

def basic_room(s,x1,z1,x2,z2,roof=8, floor_y=2):
    s.fill(x1,floor_y,z1,x2,floor_y,z2,RUNE)
    s.fill(x1,roof,z1,x2,roof,z2,STONE)
    for y in range(floor_y+1,roof):
        for x in range(x1,x2+1):
            for z in range(z1,z2+1):
                wall=(x in (x1,x2) or z in (z1,z2))
                s.set(x,y,z,(CRACK if (x*3+z*7+y)%17==0 else MOSS) if wall else AIR)
    for x,z in ((x1+1,z1+1),(x1+1,z2-1),(x2-1,z1+1),(x2-1,z2-1)):
        s.fill(x,floor_y+1,z,x,roof-1,z,RUNE)
        s.set(x,roof-1,z,GLOW)

def chest(s,x,y,z,loot='miggi_exploration:labyrinth_supplies'):
    s.set(x,y,z,'minecraft:chest',{'facing':'north'}, {'id':S('minecraft:chest'),'LootTable':S(loot)})

def build_trial():
    s=Structure('labyrinth_trial',(45,18,45))
    # Outer ruins; all air is explicitly carved so manual placement works even inside stone.
    basic_room(s,1,1,43,43,roof=13)
    # Interior split: entry (south), two guards' galleries, boss arena (north)
    s.fill(3,3,25,41,10,25,STONE)
    s.fill(20,3,25,24,6,25,AIR)
    s.fill(15,3,27,15,10,40,MOSS)
    s.fill(29,3,27,29,10,40,MOSS)
    for z in (29,36):
        s.fill(15,3,z,15,6,z,AIR)
        s.fill(29,3,z,29,6,z,AIR)
    # Arena circle-ish: octagonal rune arena using dark floor rings
    for x in range(5,40):
        for z in range(4,24):
            dx=x-22;dz=z-13
            d=dx*dx+dz*dz
            if 74<=d<=110:s.set(x,2,z,CRACK)
            if 30<=d<=37:s.set(x,2,z,GLOW)
    for x,z in ((8,5),(36,5),(8,21),(36,21)):
        s.fill(x,3,z,x,9,z,RUNE)
        s.set(x,10,z,GLOW)
    # Defensive pillars + entry supply chests
    chest(s,8,3,37)
    chest(s,36,3,37)
    for x,z in ((10,31),(34,31)):
        s.fill(x,3,z,x,5,z,CRACK)
        s.set(x,6,z,GLOW)
    # Boss spawned only in the main trial template; avoid placing repeatedly.
    s.entity(22.5,3,13.5,'miggi_exploration:minotaur')
    # Enemy guards near back rooms
    s.entity(10.5,3,34.5,'miggi_exploration:orc')
    s.entity(35.5,3,34.5,'miggi_exploration:goblin')
    # Northern vault access corridor and exit marking
    for x in range(19,26):s.set(x,3,2,GLOW)
    return s.save()

def build_vault():
    s=Structure('labyrinth_vault',(25,12,25))
    basic_room(s,1,1,23,23,roof=10)
    s.fill(3,3,12,21,7,12,MOSS)
    s.fill(10,3,12,14,6,12,AIR)
    for x,z in ((5,5),(19,5),(5,19),(19,19)):
        s.fill(x,3,z,x,6,z,CRACK)
        s.set(x,7,z,GLOW)
    for x,z,loot in ((11,6,'miggi_exploration:labyrinth_treasure'),(13,6,'miggi_exploration:labyrinth_treasure'),(12,20,'miggi_exploration:labyrinth_supplies')):
        chest(s,x,3,z,loot)
    s.fill(10,3,3,14,3,3,GLOW)
    return s.save()

if __name__=='__main__':
    info=[build_trial(),build_vault()]
    print(json.dumps(info,ensure_ascii=False,indent=2))
