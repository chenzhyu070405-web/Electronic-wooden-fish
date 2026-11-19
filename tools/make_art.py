"""Rebuild the original pixel art and wooden-fish block models. Requires Pillow."""
from pathlib import Path
from make_relic_art import build as build_relics
import math
import random
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/muyu"
rng = random.Random(8128)


def save(image, name):
    path = ROOT / name
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def lotus(draw, x, y, s=1, colors=("#a54549","#e68b6f","#ffdc9b")):
    outlines = [[(-7,-2),(-4,-2),(-2,0),(-2,2),(-5,1)],
                [(7,-2),(4,-2),(2,0),(2,2),(5,1)],
                [(-5,-5),(-2,-4),(0,0),(-1,3),(-4,1)],
                [(5,-5),(2,-4),(0,0),(1,3),(4,1)],
                [(0,-7),(3,-3),(2,1),(0,3),(-2,1),(-3,-3)]]
    for i, points in enumerate(outlines):
        draw.polygon([(x+a*s,y+b*s) for a,b in points],fill=colors[i//2])
    draw.line([(x-4*s,y+3*s),(x+4*s,y+3*s)],fill=colors[2],width=max(1,s))


def icons():
    def canvas():
        im=Image.new("RGBA",(32,32))
        return im,ImageDraw.Draw(im)
    im,d=canvas()
    d.polygon([(4,11),(8,8),(14,8),(17,10),(21,8),(26,9),(29,13),(29,20),(26,25),(23,29),(18,29),(15,27),(11,29),(7,27),(4,22),(2,16)],fill="#794128")
    d.polygon([(5,12),(10,10),(16,12),(21,10),(25,11),(27,14),(26,21),(22,26),(18,27),(15,25),(11,27),(7,23),(4,17)],fill="#e8a333")
    d.polygon([(5,13),(10,11),(15,13),(11,14),(7,20),(5,19)],fill="#ffdf79")
    d.line([(15,10),(16,5),(19,3)],fill="#684638",width=3)
    d.polygon([(18,6),(20,2),(26,2),(24,5),(20,7)],fill="#879455")
    lotus(d,18,22,colors=("#a75839","#e68b52","#fff2ad")); save(im,"textures/item/buddhas_apple.png")
    im,d=canvas()
    d.polygon([(7,23),(10,12),(23,2),(27,3),(24,17),(14,28),(6,30)],fill="#55394e")
    d.polygon([(9,24),(12,13),(24,4),(20,16)],fill="#fff2ba")
    d.polygon([(9,26),(20,16),(25,5),(22,18),(13,26)],fill="#e2a44c")
    d.polygon([(7,28),(12,20),(15,18),(12,26)],fill="#97c7b0")
    d.line([(14,15),(17,13),(18,16),(20,12)],fill="#986247")
    d.line([(4,7),(4,11)],fill="#ffe7a6");d.line([(2,9),(6,9)],fill="#ffe7a6")
    save(im,"textures/item/divine_shard.png")
    im,d=canvas()
    d.rectangle((13,2,19,9),fill="#433a39");d.rectangle((15,3,17,7),fill="#e7bb67")
    d.polygon([(5,25),(5,22),(8,18),(9,11),(12,8),(20,8),(23,11),(24,18),(27,22),(27,25)],fill="#543e38")
    d.polygon([(7,23),(10,19),(11,12),(13,10),(19,10),(21,12),(22,19),(25,23)],fill="#c99249")
    d.rectangle((10,14,22,15),fill="#ffe4a0"); d.rectangle((7,22,25,23),fill="#f1cd80")
    d.rectangle((14,25,18,28),fill="#9b433b");d.rectangle((15,27,18,30),fill="#de7255")
    lotus(d,16,19,colors=("#8e6141","#b27643","#fff4bc")); save(im,"textures/item/destiny_bell.png")
    im,d=canvas();lotus(d,16,18,2);save(im,"textures/particle/lotus.png")


def effigy():
    im=Image.new("RGB",(128,128)); d=ImageDraw.Draw(im)
    for y in range(128):
        for x in range(128):
            if y>=98: palette=["#ae7840","#ca944b","#edbf68"]
            elif y>=64 and x<48: palette=["#333447","#44404b","#655046"]
            elif y>=64: palette=["#bf6759","#df8c70","#f1b78c"]
            elif x>=88 and y>=32: palette=["#c7984b","#e2b764","#ffe09a"]
            elif x>=48: palette=["#a84d36","#bc5c3b","#ce7649"]
            else: palette=["#dbab6d","#e5be84","#edc991"]
            if y>=64 and x<48: idx=((x//2 + y//2)%3)
            elif x>=48 and y<62: idx=1 + (1 if (x+y//2)%12<2 else -1 if (x+y//2)%12==7 else 0)
            else: idx=rng.choices([0,1,2],[1,15,2])[0]
            d.point((x,y),fill=palette[idx])
    # Cube front occupies U=8..17, V=8..17.
    d.rectangle((8,8,17,17),fill="#edc78e")
    d.point((12,9),fill="#fff3be");d.point((13,9),fill="#ffdf82")
    d.line((8,11,10,11),fill="#875c43");d.line((15,11,17,11),fill="#875c43")
    d.line((9,12,11,12),fill="#694b41");d.line((14,12,16,12),fill="#694b41")
    d.line((12,12,12,14),fill="#dbad76");d.point((13,14),fill="#f8d99e")
    d.line((11,16,14,16),fill="#b57258")
    save(im,"textures/entity/buddha.png")


def hall():
    w,h=512,288
    im=Image.new("RGB",(w,h));d=ImageDraw.Draw(im)
    for y in range(h):
        f=round(min(1,y/192)*32)/32
        d.line((0,y,w,y), fill=tuple(int(a+(b-a)*f) for a,b in zip((151,101,77),(255,224,157))))
    # The aureole is engraved into the back wall of the court.
    cx,cy=256,80
    for r,color in [(67,"#e4b775"),(64,"#efc985"),(58,"#d9ac68"),(55,"#f4d99a"),(52,"#f7dfa5")]:
        d.ellipse((cx-r,cy-r,cx+r,cy+r),fill=color)
    for i in range(32):
        a=i*math.tau/32
        def dot(r,b=0):return (round(cx+r*math.cos(a+b)),round(cy+r*math.sin(a+b)))
        d.polygon([dot(60,-0.014),dot(66),dot(60,0.014)], fill="#fff0b7")
        d.line([dot(70),dot(76 if i%4 else 82)],fill="#e9c889",width=1)
    for layer,(base,amp,color) in enumerate([(151,25,"#dec291"),(165,21,"#ccb486"),(179,14,"#b4a583")]):
        pts=[(x,round(base+amp*(0.6*math.sin(x/42+layer)+0.4*math.cos(x/15+layer*2)))) for x in range(-8,w+9,4)]
        d.polygon(pts+[(w,h),(0,h)],fill=color)
    for i in range(56):
        x=rng.randrange(-30,520);y=rng.randrange(146,189);ww=rng.randrange(16,72)
        d.rounded_rectangle((x,y,x+ww,y+rng.randrange(2,7)),3,fill=rng.choice(["#f0d8ac","#e9d0a2","#f6e4bb"]))
    d.polygon([(0,219),(142,182),(370,182),(512,219),(512,288),(0,288)],fill="#d1a875")
    for x in range(-500,1001,75):
        d.line([(cx+(x-cx)*0.16,182),(x,288)],fill="#b98d61")
    for y in [188,198,214,237,269]:
        d.line((0,y,512,y), fill="#bf9364")
    for step in range(4):
        left=171+step*5;right=w-left;y=214-step*4
        d.polygon([(left,y),(right,y),(right-12,y-4),(left+12,y-4)],fill="#f1cd8c")
        d.line((left,y+1,right,y+1),fill="#aa8059",width=2)
    for x in [18,64,448,494]:
        d.rectangle((x-4,17,x+5,199),fill="#866549");d.rectangle((x-2,19,x+1,198),fill="#c59660")
        for y in [21,29,175,191,197]:d.rectangle((x-7,y,x+7,y+2),fill="#edc581")
        d.rectangle((x-8,199,x+9,203),fill="#967953")
    for x in [45,466]:
        d.line((x,15,x,54),fill="#775640")
        d.rectangle((x-6,51,x+6,54),fill="#c39657");d.rounded_rectangle((x-8,55,x+8,68),4,fill="#bb6846")
        d.rectangle((x-4,55,x+4,68),fill="#e79954");d.line((x-6,69,x+6,69),fill="#f3c575",width=2)
        d.line((x,70,x,79),fill="#ac6842",width=2)
    for x in [93,406]:
        d.rectangle((x-8,1,x+8,63),fill="#ad674a")
        d.polygon([(x-8,62),(x,70),(x+8,62)],fill="#ad674a")
        for dx in [-6,6]:d.line((x+dx,5,x+dx,62),fill="#e6b575")
        lotus(d,x,45,colors=("#d49663","#edb574","#ffe0a1"))
    for side in [False,True]:
        pts=[(0,0),(152,0),(116,9),(90,13),(52,14),(27,20),(0,20)]
        if side: pts=[(w-x,y) for x,y in pts]
        d.polygon(pts,fill="#694e44")
        d.line(pts[2:], fill="#c29965",width=3)
    save(im,"textures/gui/cloud_hall.png")
    cloud=Image.new("RGBA",(96,20));q=ImageDraw.Draw(cloud)
    for x,y,r in [(14,12,8),(26,10,10),(41,8,13),(55,13,9),(69,13,7),(78,14,5)]:
        q.ellipse((x-r,y-r//2,x+r,y+r//2),fill=(255,242,207,174))
    q.line((3,16,90,16),fill=(255,239,205,140),width=2)
    save(cloud,"textures/gui/cloud.png")


if __name__ == "__main__":
    build_relics()
    # The original six 32px materials preceded the seeded effigy and cloud paintings.
    for _ in range(6 * 32 * 32):
        rng.random()
    icons(); effigy(); hall()
