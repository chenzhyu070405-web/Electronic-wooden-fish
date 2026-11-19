"""Generate scripture-inspired gold equipment on the vanilla sprites' silhouettes and armor UV maps."""
import argparse
import io
import json
import math
from pathlib import Path
import zipfile
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources"
ASSETS = ROOT / "assets/muyu"
DATA = ROOT / "data"
PATTERNS = {
    "helmet": ["SSS", "S S"], "chestplate": ["S S", "SSS", "SSS"],
    "leggings": ["SSS", "S S", "S S"], "boots": ["S S", "S S"],
    "horse_armor": ["S S", "SLS", "SSS"], "nautilus_armor": [" SS", "SNS", " SS"],
    "sword": ["S", "S", "|"], "spear": ["  S", " | ", "|  "],
    "pickaxe": ["SSS", " | ", " | "], "axe": ["SS", "S|", " |"],
    "shovel": ["S", "|", "|"], "hoe": ["SS", " |", " |"],
}

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")

def png(path, image):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)

SCALE = 3
GOLD = ["#955b23", "#b3782c", "#d59b39", "#eab947", "#f3cb62", "#ffe195", "#fff1bc"]
INK, BRONZE, LEAF, LIGHT, RED = "#75431c", "#a96a25", "#f4c253", "#fff0ae", "#a6382e"

def wheel(d, x, y, r):
    x,y,r = [round(n*SCALE) for n in (x,y,r)]
    d.ellipse((x-r,y-r,x+r,y+r),fill=INK)
    d.ellipse((x-r+1,y-r+1,x+r-1,y+r-1),fill=LIGHT)
    d.ellipse((x-r+3,y-r+3,x+r-3,y+r-3),fill=BRONZE)
    for i in range(8):
        angle = i*math.tau/8
        d.line((x,y,x+round((r-2)*math.cos(angle)),y+round((r-2)*math.sin(angle))),fill=LIGHT)
    d.rectangle((x-1,y-1,x+1,y+1),fill=RED)

def lotus(d,x,y,size):
    def poly(points, color):
        d.polygon([(round((x+a*size)*SCALE),round((y+b*size)*SCALE)) for a,b in points],fill=color)
    for side in (-1,1):
        poly([(0,1.8),(3.2*side,1.5),(5*side,-1.8),(2.5*side,-1.5),(0,0)],INK)
        poly([(.3*side,1),(2.8*side,.8),(4.1*side,-1.2),(2.1*side,-.8)],LEAF)
        poly([(0,1),(2.4*side,-.1),(3.1*side,-3.0),(1.1*side,-2),(0,-.3)],BRONZE)
        poly([(.5*side,.45),(2*side,-.6),(2.5*side,-2.2),(1.2*side,-1.3)],LIGHT)
    poly([(0,-4.1),(1.55,-1.1),(1,.45),(0,1.35),(-1,.45),(-1.55,-1.1)],INK)
    poly([(0,-3.2),(.85,-1.05),(0,.55),(-.85,-1.05)],LIGHT)
    poly([(-2.8,2),(2.8,2),(2,2.7),(-2,2.7)],BRONZE)

def gold_surface(z, source, dest, kind, worn=False):
    base = Image.open(io.BytesIO(z.read(source))).convert("RGBA")
    alpha = base.getchannel("A")
    lum = base.convert("L")
    levels = sorted(lum.getpixel((x,y)) for y in range(base.height) for x in range(base.width) if alpha.getpixel((x,y))>127)
    breaks = [levels[round((len(levels)-1)*p)] for p in (.08,.24,.42,.63,.82,.955)]
    metal = Image.new("RGB",base.size)
    d = ImageDraw.Draw(metal)
    for y in range(base.height):
        for x in range(base.width):
            value = lum.getpixel((x,y))
            d.point((x,y), fill=GOLD[sum(value>cut for cut in breaks)])
    metal = metal.resize((metal.width*SCALE,metal.height*SCALE),Image.Resampling.NEAREST)
    d = ImageDraw.Draw(metal)
    def line(points, color, width=1):
        d.line([(round(x*SCALE),round(y*SCALE)) for x,y in points],fill=color,width=width)
    if worn and kind=="humanoid":
        # Forehead, chest, back, shoulders and toes occupy separate islands of the vanilla UV.
        for x in (12,28):
            lotus(d,x,9.55,.68)
        line([(20,20.2),(21.4,21.7),(24,22.7),(26.6,21.7),(28,20.2)],INK,2)
        line([(20,20),(21.4,21.4),(24,22.3),(26.6,21.4),(28,20)],LIGHT,2)
        wheel(d,23.8,25.9,2.9)
        lotus(d,23.8,29.6,.58)
        lotus(d,35.8,25.6,.83)
        for x in (42,46,50,54):
            lotus(d,x,22.9,.38)
        for x in (2,6,10,14):
            lotus(d,x,29.2,.37)
    elif worn and kind=="humanoid_leggings":
        for x in (6,14):
            lotus(d,x,23.7,.43)
        for y in (27.7,28.5,30.2):
            line([(16,y),(40,y)],LIGHT if y==28.5 else BRONZE,2)
        wheel(d,23.8,29.45,1.9)
    elif worn:
        # Small repeated appliqués keep the saddle/nautilus panels legible across UV seams.
        for row,y in enumerate(range(5,base.height,11)):
            for x in range(4+(row%2)*5,base.width,12):
                if row%2: wheel(d,x,y,2.6)
                else: lotus(d,x,y,.68)
    elif kind in ("helmet","chestplate","leggings","boots","horse_armor","nautilus_armor"):
        if kind=="chestplate":
            wheel(d,8,8.4,3.3); lotus(d,8,12.5,.48)
        elif kind=="helmet":
            lotus(d,8,6.2,.91)
        elif kind=="leggings":
            wheel(d,8,4.5,1.9); lotus(d,4.8,9,.40); lotus(d,11.4,9,.40)
        elif kind=="boots":
            lotus(d,4.4,8.7,.43); lotus(d,11.2,8.7,.43)
        elif kind=="horse_armor":
            wheel(d,7.2,10.6,2.7)
        else:
            wheel(d,8.6,7.8,3.6); lotus(d,8.7,12.2,.60)
    else:
        # Cinnabar silk bindings occupy the grip; gold edging and a lotus at the socket identify the set.
        reverse = kind=="spear_in_hand"
        if reverse:
            for t in (13,14,23,24): line([(t-1,t+1),(t+1,t-1)],RED,2)
            lotus(d,4.6,4.0,.57)
        else:
            for t in (2.8,4.1,5.4):
                line([(t-1,14.8-t),(t+1,16.8-t)],RED,2)
            socket={"axe":(9.6,5.0),"hoe":(10.2,3.5),"pickaxe":(11.7,4.1),
                    "shovel":(10.2,5.6),"sword":(6.2,9.3),"spear":(10.9,5.1)}[kind]
            lotus(d,*socket,.52)
    metal.putalpha(alpha.resize(metal.size, Image.Resampling.NEAREST))
    png(dest,metal)

def model_tree(z, node, kind):
    if isinstance(node, list): return [model_tree(z,item,kind) for item in node]
    if not isinstance(node, dict): return node
    node = {k:model_tree(z,v,kind) for k,v in node.items()}
    if node.get("type") == "minecraft:model":
        original = node["model"]
        short = original.split("/")[-1]
        new = short.replace("netherite_"+kind,"divine_"+kind,1)
        overlay = "divine_"+kind+("_in_hand" if short.endswith("_in_hand") else "")
        write(ASSETS/f"models/item/{new}.json", {"parent":original,
              "textures":{"layer0":"muyu:item/"+overlay}})
        node["model"]="muyu:item/"+new
    return node

def generate(z):
    def read(name): return json.loads(z.read(name))
    for kind,pattern in PATTERNS.items():
        item = "divine_"+kind
        item_id = "muyu:"+item
        definition = read("assets/minecraft/items/netherite_"+kind+".json")
        write(ASSETS/f"items/{item}.json",model_tree(z,definition,kind))
        for name in [kind]+(["spear_in_hand"] if kind=="spear" else []):
            gold_surface(z,f"assets/minecraft/textures/item/netherite_{name}.png",ASSETS/f"textures/item/divine_{name}.png",name)
        symbols = {"S":"muyu:divine_shard", "|":"minecraft:stick", "L":"minecraft:leather", "N":"minecraft:nautilus_shell"}
        write(DATA/f"muyu/recipe/{item}.json", {"type":"minecraft:crafting_shaped","category":"equipment",
              "pattern":pattern,"key":{k:v for k,v in symbols.items() if k in "".join(pattern)},"result":{"id":item_id}})
        write(DATA/f"muyu/advancement/recipes/{item}.json", {"parent":"minecraft:recipes/root",
              "criteria":{"has_shard":{"trigger":"minecraft:inventory_changed","conditions":{"items":[{"items":"muyu:divine_shard"}]}},
                          "has_recipe":{"trigger":"minecraft:recipe_unlocked","conditions":{"recipes":item_id}}},
              "requirements":[["has_shard","has_recipe"]],"rewards":{"recipes":[item_id]}})
    for member in z.namelist():
        if not member.startswith("data/minecraft/tags/item/") or not member.endswith(".json"): continue
        values=read(member)["values"]
        added=["muyu:divine_"+k for k in PATTERNS if "minecraft:netherite_"+k in values]
        if added:
            if member.endswith("/swords.json"): added.insert(0,"muyu:godslayer")
            write(ROOT/member,{"replace":False,"values":added})
    equipment=read("assets/minecraft/equipment/netherite.json")
    for layer in equipment["layers"]:
        gold_surface(z,f"assets/minecraft/textures/entity/equipment/{layer}/netherite.png",
                     ASSETS/f"textures/entity/equipment/{layer}/divine.png",layer,True)
        equipment["layers"][layer]=[{"texture":"muyu:divine"}]
    equipment["trim_overrides"]=read("assets/minecraft/equipment/gold.json")["trim_overrides"]
    write(ASSETS/"equipment/divine.json",equipment)
    # Neutral pixels allow the individual modeled leaf bands to supply their own gold colors.
    im=Image.new("RGB",(16,16))
    for y in range(16):
        for x in range(16):
            v=245 + int(5*math.sin(.35*x+.18*y))
            im.putpixel((x,y),(v,v,v))
    png(ASSETS/"textures/effect/lotus_gold.png",im)

if __name__ == "__main__":
    ap=argparse.ArgumentParser()
    ap.add_argument("vanilla_client_jar",type=Path)
    with zipfile.ZipFile(ap.parse_args().vanilla_client_jar) as vanilla: generate(vanilla)
