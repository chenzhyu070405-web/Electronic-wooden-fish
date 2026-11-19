"""Redwood block, mallet poses and lotus greatsword sprites. Run with Python and Pillow."""
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw

ASSETS = Path(__file__).resolve().parents[1] / "src/main/resources/assets/muyu"
TOP = 9.95
LEFT, RIGHT, FRONT, BACK, BASE = 1.40, 14.60, 1.90, 14.10, 1.15
SLOT_L, SLOT_R, SLOT_LOW, SLOT_HIGH, SLOT_BACK = 3.60, 12.40, 4.42, 5.37, 3.35
FACES = ("north", "south", "east", "west", "up", "down")


def save(im, path):
    dest = ASSETS / path
    dest.parent.mkdir(parents=True, exist_ok=True)
    im.save(dest)


def timber(top=False):
    """Broad longitudinal grain. Bent streaks are nested around the same knot, like a cut board."""
    im = Image.new("RGB", (64, 64))
    colors = ["#643422", "#773d29", "#84432c", "#91492f", "#9a5135", "#a65d3c", "#b96f48"]
    for y in range(64):
        for x in range(64):
            bow = 6.3*math.exp(-((x-39)/18)**2) + 1.9*math.sin(x/24)
            flow = y + bow*(.55 + .45*math.cos((y-31)/17)) + 1.2*math.sin(x/19+y/21)
            rings = (flow + 1.8*math.sin(flow*.21)) % (14 if top else 19)
            broad = .5 + .42*math.sin(flow/10 - x/74)
            level = 3 + int(broad*1.6)
            if rings < 1.3: level = 1 if top else 2
            elif rings < 3.1: level = 2 if top else 3
            elif rings < 4.1: level = 5
            if not top and y > 51 and level > 2: level -= 1
            im.putpixel((x,y), tuple(bytes.fromhex(colors[level][1:])))
    d = ImageDraw.Draw(im)
    if top:
        d.line([(0,35),(10,35),(18,34),(24,32),(30,29),(36,27),(42,26),(48,26),
                (54,27),(60,29),(63,29)], fill="#79402e",width=2)
        d.line([(14,35),(22,34),(30,31),(36,29),(42,28),(47,28)],fill="#ad6140")
        d.line([(31,31),(38,30),(42,30),(49,29),(54,30)],fill="#85442f")
        d.line([(0,8),(9,8),(15,7),(22,6)],fill="#bd744d")
        d.line([(53,49),(58,50),(63,50)],fill="#ba6d47")
    return im


def carved_side():
    im = timber()
    d = ImageDraw.Draw(im)
    # An incised lower shadow and a warm upper lip give the curved koi and the cloud scrolls a cut edge.
    outline = [(12,38),(14,31),(19,33),(23,31),(27,26),(34,22),(41,22),(47,25),
               (50,28),(49,31),(44,34),(37,34),(31,31),(28,31),(24,35),(19,37),
               (17,42),(15,45),(14,40),(10,42),(9,40),(12,38)]
    mask = Image.new("1", im.size)
    ImageDraw.Draw(mask).polygon(outline, fill=1)
    for y in range(21,46):
        for x in range(9,51):
            if mask.getpixel((x,y)):
                r,g,b = im.getpixel((x,y))
                im.putpixel((x,y),(r+13,g+9,b+5))
    paths = [
        outline,
        [(27,26),(29,20),(33,17),(34,21),(38,19),(38,22)],
        [(32,32),(32,37),(35,39),(36,34)],
        [(42,24),(40,26),(40,29),(43,32)],
        [(21,34),(25,32),(29,28),(34,25),(38,25)],
        [(27,30),(30,28),(33,27)], [(34,28),(35,30),(38,29)],
        [(10,20),(14,20),(17,18),(17,15),(15,14),(13,15),(13,17),(15,17)],
        [(36,44),(42,44),(45,42),(47,39),(51,38),(54,39),(55,41),
         (53,43),(50,43),(49,41),(51,40)],
        [(46,47),(51,46),(56,46)],
    ]
    for path in paths:
        d.line([(x,y+1) for x,y in path],fill="#693823",width=2)
    for path in paths:
        d.line(path,fill="#c38a5e",width=2)
    d.line([(27,26),(34,22),(40,22)], fill="#d29462")
    d.rectangle((45,26,46,27),fill="#542e22")
    d.point((45,25),fill="#d39b69")
    return im


def wood_textures():
    save(timber(True), "textures/block/lacquer.png")
    front = timber()
    save(front, "textures/block/rosewood_face.png")
    # Continuous UV across the front quads. Keep the lower lip visible even in a level view.
    y = math.ceil(64*(TOP-SLOT_LOW)/(TOP-BASE))
    x0 = math.ceil(64*(RIGHT-SLOT_R)/(RIGHT-LEFT))
    x1 = math.floor(64*(RIGHT-SLOT_L)/(RIGHT-LEFT))-1
    ImageDraw.Draw(front).line((x0,y,x1,y), fill="#bf7950")
    save(front, "textures/block/slot_face.png")
    save(carved_side(), "textures/block/carved.png")
    for name, colors in [
        ("mouth", ("#1e1413","#251816","#30201b")),
        ("mallet", ("#c49462","#dcaf7b","#e7c48c")),
        ("mallet_handle", ("#492d22","#593827","#714831")),
        ("gold", ("#977441","#ba9458","#d1ae70")),
    ]:
        im = Image.new("RGB",(32,32),colors[1])
        d = ImageDraw.Draw(im)
        for y in (4,13,24):
            path=[(0,y),(8,y),(14,y-1),(20,y-1),(26,y),(31,y)]
            d.line(path, fill=colors[0],width=1)
            if name != "mouth":
                d.line([(x,py+1) for x,py in path][2:5], fill=colors[2])
        save(im,"textures/block/"+name+".png")
    cushion = Image.new("RGB",(32,32),"#742f39")
    d=ImageDraw.Draw(cushion)
    for y in range(32):
        for x in range(32):
            if (x + y) % 8 == 0 or (x-y) % 8 == 0: d.point((x,y),fill="#8a4147")
            elif (x%4,y%4)==(2,1): d.point((x,y),fill="#652d36")
    d.rectangle((0,0,31,31),outline="#9e5656")
    save(cushion,"textures/block/brocade.png")


def fish_model(struck):
    elements = []
    def face(tex, uv=(0,0,16,16)):
        return {"texture":"#"+tex, "uv":[round(v,5) for v in uv]}
    def element(name, start, end, faces, rotation=None):
        obj = {"name":name, "from":start, "to":end, "faces":faces}
        if rotation: obj["rotation"] = rotation
        elements.append(obj)
    def solid(name, start, end, tex, rotation=None):
        element(name,start,end,{s:face(tex) for s in FACES},rotation)

    solid("antique gold lower hem", [.65,0,.65], [15.35,.28,15.35], "gold")
    element("brocade square cushion", [.7,.28,.7], [15.3,BASE,15.3],
            {s:face("brocade",(0,12,16,16) if s not in ("up","down") else (0,0,16,16)) for s in FACES})
    # One continuous cuboid on five faces. Only the front skin is cut around the actual cavity.
    element("single redwood body",[LEFT,BASE,FRONT],[RIGHT,TOP,BACK],
            {"up":face("lacquer"),"down":face("plain"),"south":face("plain"),
             "east":face("carved"),"west":face("carved")})
    def front_piece(name,x0,y0,x1,y1):
        uv=(16*(RIGHT-x1)/(RIGHT-LEFT),16*(TOP-y1)/(TOP-BASE),
            16*(RIGHT-x0)/(RIGHT-LEFT),16*(TOP-y0)/(TOP-BASE))
        element(name,[x0,y0,FRONT],[x1,y1,FRONT],{"north":face("front",uv)})
    front_piece("above slot",LEFT,SLOT_HIGH,RIGHT,TOP)
    front_piece("below slot",LEFT,BASE,RIGHT,SLOT_LOW)
    front_piece("left end",LEFT,SLOT_LOW,SLOT_L,SLOT_HIGH)
    front_piece("right end",SLOT_R,SLOT_LOW,RIGHT,SLOT_HIGH)
    def plane(name,start,end,side,tex,uv=(0,0,16,16)):
        element(name,start,end,{side:face(tex,uv)})
    plane("shadowed slot back",[SLOT_L,SLOT_LOW,SLOT_BACK],[SLOT_R,SLOT_HIGH,SLOT_BACK],"north","mouth")
    plane("slot ceiling",[SLOT_L,SLOT_HIGH,FRONT],[SLOT_R,SLOT_HIGH,SLOT_BACK],"down","mouth")
    plane("slot left wall",[SLOT_L,SLOT_LOW,FRONT],[SLOT_L,SLOT_HIGH,SLOT_BACK],"east","mouth")
    plane("slot right wall",[SLOT_R,SLOT_LOW,FRONT],[SLOT_R,SLOT_HIGH,SLOT_BACK],"west","mouth")
    plane("slot floor",[SLOT_L,SLOT_LOW,FRONT+.25],[SLOT_R,SLOT_LOW,SLOT_BACK],"up","mouth")
    plane("lit redwood lip",[SLOT_L,SLOT_LOW,FRONT],[SLOT_R,SLOT_LOW,FRONT+.25],"up","lacquer",(0,1.5,16,2.5))

    # The idle grip touches the rear of the board. In the strike pose the head's flat underside is at TOP.
    centre_y = TOP + (1.30 if struck else .46)
    pivot = [13.2,centre_y,12.75]
    rot = {"origin":pivot,"y":-18,"z":0 if struck else -11}
    solid("light boxwood head",[2.8,centre_y-1.30,11.44],[6.0,centre_y+1.30,14.06],"mallet",rot)
    solid("dark mallet shaft",[6.0,centre_y-.43,12.32],[13.2,centre_y+.43,13.18],"handle",rot)
    model = {"parent":"minecraft:block/block","textures":{"plain":"muyu:block/rosewood_face","front":"muyu:block/slot_face","handle":"muyu:block/mallet_handle",
             **{k:"muyu:block/"+k for k in ("lacquer","carved","mouth","brocade","gold","mallet")},
             "particle":"muyu:block/rosewood_face"},"elements":elements}
    name = "wooden_fish_struck" if struck else "wooden_fish"
    (ASSETS/("models/block/"+name+".json")).write_text(json.dumps(model,indent=2)+"\n",encoding="utf-8")


def sword_icon():
    im=Image.new("RGBA",(64,64))
    d=ImageDraw.Draw(im)
    # a follows the blade towards its point; b crosses its spine. Draw in weapon coordinates.
    def at(a,b):
        return round(19.5 + (a+b)*math.sqrt(.5)), round(43.2 + (b-a)*math.sqrt(.5))
    def poly(coords,color):
        d.polygon([at(*p) for p in coords],fill=color)
    def line(coords,color,width=1):
        d.line([at(*p) for p in coords],fill=color,width=width)

    profile=[(1.0,3.6),(6,3.2),(11,5.4),(15.4,10.2),(23.2,10.5),(21.0,7.4),
             (35,7.1),(39,7.3),(48.5,4.6),(59.0,0)]
    outline=[(a,-b) for a,b in profile]+list(reversed(profile[:-1]))
    poly(outline,"#16333f")
    for sign, bevel, steel in [(-1,"#d1e4e8","#32626a"),(1,"#8eaeb9","#153e4b")]:
        border=[(a,sign*b) for a,b in profile]
        poly([(1,0)]+border+[(1,0)],bevel)
        inner=[(a-.4,sign*max(0,b-1.8)) for a,b in profile[:-1]]+[(55.7,0)]
        poly([(1,0)]+inner+[(1,0)],steel)
        line(border[2:5] if sign == 1 else border[1:5],"#edf6f5")
        line(border[-3:], "#f4fbf6" if sign==-1 else "#c0d9df")
    poly([(2,0),(17,-2.0),(36,-.9),(54,0),(28,.65),(12,.9)],"#254e56")
    line([(8,-.5),(17,-1),(29,-.8),(39,0)],"#52777a")
    # Angular gilt inlay ends well below the clean point.
    line([(2,0),(13,0),(19,1),(25,0),(31,0)],"#785330",2)
    line([(5,-.4),(15,-.4),(20,.2),(27,0)],"#c39b54")
    for sign in (-1,1):
        line([(10,0),(13,sign*2),(14,sign*4),(19,sign*5.3)],"#ac8145",2)
        line([(18,0),(21,sign*2.4),(25,sign*3.2)],"#bb975a")
        line([(14,sign*3),(16,sign*4.6)],"#efd295")

    poly([(-18,-3),(-5,-3.4),(0,-2.9),(0,2.9),(-5,3.4),(-18,3)],"#412731")
    poly([(-18,-1.7),(-4,-2.4),(-2,1.4),(-18,1.8)],"#893841")
    for a in (-16,-12.8,-9.6,-6.5):
        line([(a,-2.1),(a+2.0,0),(a+2.7,2.0)],"#c3665c")
        line([(a+.8,-2.2),(a+2.9,0),(a+3.5,1.8)],"#552a33")
    for a in (-15.4,-7):
        poly([(a-1,-2.9),(a+.7,-2.9),(a+.7,2.9),(a-1,2.9)],"#b78746")
        line([(a+.6,-2.7),(a+.6,.3)],"#ffe39c")
    # Short knotted silk and a cut gold pommel.
    d.line([(5,55),(4,58),(6,60),(5,62)],fill="#682738",width=2)
    d.polygon([(5,56),(8,58),(6,60),(3,59)],fill="#b54242")
    d.point((5,58),fill="#f0a172")
    d.line([(6,60),(9,62),(11,62)],fill="#9c3238",width=2)
    d.line([(7,60),(10,61)],fill="#db6051")
    poly([(-22,0),(-19.5,-3.7),(-17,-3.2),(-15.7,0),(-17,3.3),(-19.5,3.6)],"#673e27")
    poly([(-21,0),(-19.3,-3),(-17,-2.2),(-16.6,0),(-18.6,1.2)],"#e4b45e")
    poly([(-20.8,0),(-18.7,1),(-16.6,0),(-18,2.6),(-19.6,2.4)],"#ad7739")
    line([(-20,-1.1),(-19.1,-2.5),(-17.4,-2.0)],"#ffdf91")

    # The rachis of each petal turns back up the blade. Paint the near tier across the far tier.
    for side in (-1,1):
        def leaf(points, color):
            poly([(a,b*side) for a,b in points],color)
        def rib(points,color,width=1):
            line([(a,b*side) for a,b in points],color,width)
        leaf([(-5,1),(-5.8,6.3),(-4.4,10.7),(-1.8,14.3),(3.8,17.3),(9.2,17.7),
              (6.2,14.2),(5.2,11.8),(1.7,8.4),(.2,4.4)],"#593820")
        leaf([(-4.5,2.4),(-4.5,6.7),(-2.8,10.6),(.4,14.1),(4.1,16.0),(7.6,16.8),
              (4.2,13.7),(2.0,11.2),(-.2,8.8)],"#ab7134")
        leaf([(-2.8,3),(-1.5,8),(1.7,11.4),(4.3,13.5),(8.2,17.0),
              (5.6,12.6),(2.0,8.8),(.3,5.5)],"#edb74f")
        rib([(-2.9,5.4),(-1.0,8.9),(2.9,12.6),(7.8,16.8)],"#ffe39a")
        leaf([(-4,1.3),(-3,6),(-.6,9.6),(4.3,11.6),(10.0,11.8),(7.1,8.6),
              (5.5,5.8),(1.2,3.4)],"#694125")
        leaf([(-2.7,2.1),(-1.6,5.7),(1.1,8.7),(5.7,10.3),(8.8,11.0),
              (5.2,7.1),(2.3,5.3)],"#dca145")
        rib([(-1.2,3.7),(1.8,6.9),(5,8.6),(8.5,10.9)],"#ffe7a4",2)
        leaf([(-1.7,.8),(.8,4.2),(5.7,6.5),(11.1,6.3),(8.6,3.6),(6.4,1.6)],"#8a592e")
        leaf([(.3,1.0),(3.6,4.3),(8.9,5.6),(6.5,2.7)],"#f9cb6a")
        rib([(2.7,2.1),(6.3,4.1),(10.4,6.0)],"#fff1ba")
    poly([(1,-3.6),(6,-3.2),(11.9,0),(6,3.1),(1,3.6)],"#98652e")
    poly([(2,-2.7),(6.2,-2.2),(10.6,0),(4,0)],"#ffe6a2")
    # Bezel and asymmetric ruby facets remain legible at 16 pixels.
    poly([(-5.6,0),(-2.6,-5.1),(1.9,-5.1),(5.5,0),(2.4,5.2),(-2.5,5.2)],"#694324")
    poly([(-4.7,0),(-2.4,-4.5),(1.6,-4.4),(4.7,0),(1.8,4.4),(-2.1,4.6)],"#f6cf74")
    poly([(-3.8,0),(-1.9,-3.65),(1.3,-3.7),(3.9,-.1),(1.5,3.6),(-1.8,3.6)],"#811d36")
    poly([(-2.9,-.8),(-1.7,-3),(1.2,-3.1),(2.7,-.55),(.1,.85)],"#dd4350")
    poly([(-2.9,0),(.1,.85),(2.7,-.55),(1.1,2.8),(-1.7,2.8)],"#a82c43")
    line([(-1.8,-2.2),(.2,-2.8),(1.1,-2.5)],"#ff9390")
    save(im,"textures/item/godslayer.png")


def sword_gui():
    # Preserve the sharp pixel palette. Use area coverage for the silhouette so diagonals
    # do not disappear when the item renderer samples a smaller inventory sprite.
    full = Image.open(ASSETS / "textures/item/godslayer.png").convert("RGBA")
    nearest = full.resize((32,32), Image.Resampling.NEAREST)
    area = full.resize((32,32), Image.Resampling.BOX)
    for y in range(32):
        for x in range(32):
            average = area.getpixel((x,y))
            sample = nearest.getpixel((x,y))
            rgb = sample[:3] if sample[3] else average[:3]
            nearest.putpixel((x,y), (*rgb, 255 if average[3] >= 74 else 0))
    d = ImageDraw.Draw(nearest)
    # Redraw the bezel at this scale: its red core occupies a complete hotbar pixel.
    d.polygon([(7,21),(9,19),(11,19),(12,21),(11,23),(9,24),(7,23)],fill="#e6b258")
    d.polygon([(8,21),(9,20),(11,20),(11,22),(10,23),(8,23)],fill="#a72a40")
    d.line([(8,21),(9,20),(10,20)],fill="#fa7272")
    d.point((9,21),fill="#df424d")
    save(nearest,"textures/item/godslayer_gui.png")


def mod_icon():
    im=Image.new("RGB",(64,64),"#35272c")
    d=ImageDraw.Draw(im)
    def panel(points,color): d.polygon(points,fill=color)
    panel([(3,40),(28,29),(61,38),(61,44),(36,55),(3,47)],"#a47b49")
    panel([(3,39),(27,28),(61,37),(61,42),(36,53),(3,45)],"#873f46")
    panel([(8,22),(30,12),(56,20),(35,31)],"#aa6141")
    panel([(8,22),(35,31),(35,49),(8,40)],"#8c442f")
    panel([(35,31),(56,20),(56,38),(35,49)],"#75402c")
    d.line([(13,34),(30,40)],fill="#34221e",width=2)
    d.line([(13,36),(30,42)],fill="#ae6a47")
    d.line([(14,21),(22,23),(29,24),(38,24),(44,25)],fill="#88472f")
    d.line([(22,17),(29,19),(37,20),(43,20)],fill="#cc8153")
    d.line([(40,36),(43,32),(47,30),(50,30),(48,33),(45,34),(42,34),(40,37)],fill="#af744e")
    d.line([(28,17),(48,16)],fill="#583528",width=3)
    panel([(22,15),(26,12),(32,14),(32,18),(28,21),(22,19)],"#bd8b5e")
    panel([(22,15),(26,12),(32,14),(28,17)],"#e5bb85")
    save(im,"icon.png")


def build():
    wood_textures()
    for struck in (False,True):
        fish_model(struck)
    sword_icon()
    sword_gui()
    mod_icon()


if __name__ == "__main__":
    build()
