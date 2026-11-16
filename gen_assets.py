import json, os, zlib, struct, random
R = "src/main/resources"
A = f"{R}/assets/tarot"
D = f"{R}/data/tarot"

def png(path, w, h, px):
    raw = b"".join(b"\x00" + b"".join(bytes(px[y][x]) for x in range(w)) for y in range(h))
    def ch(t, d): return struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d) & 0xffffffff)
    open(path, "wb").write(b"\x89PNG\r\n\x1a\n" + ch(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)) + ch(b"IDAT", zlib.compress(raw)) + ch(b"IEND", b""))

def card_tex(path, color, seed, glyph_color=(255,240,200,255)):
    T = (0,0,0,0); border=(212,175,55,255); body=tuple(color)+(255,); dark=tuple(int(c*.55) for c in color)+(255,)
    px = [[T]*16 for _ in range(16)]
    for y in range(1,15):
        for x in range(3,13):
            px[y][x] = body
    for y in range(1,15):
        for x in range(3,13):
            if y in (1,14) or x in (3,12): px[y][x] = border
    for y in range(2,14):
        for x in range(4,12):
            if y in (2,13) or x in (4,11): px[y][x] = dark
    rnd = random.Random(seed)
    for y in range(4,12):          # glifo simétrico 3x4 espejado
        for x in range(5,8):
            if rnd.random() < .5:
                px[y][x] = glyph_color; px[y][15-x] = glyph_color
    png(path, 16, 16, px)

cards = {
 "fool":(120,200,120), "magician":(200,60,60), "empress":(80,170,90), "chariot":(70,110,200),
 "wheel":(230,170,40), "tower":(90,90,110), "star":(80,150,230), "sun":(250,140,30)}
names = {
 "fool":("El Loco","The Fool"), "magician":("El Mago","The Magician"), "empress":("La Emperatriz","The Empress"),
 "chariot":("El Carro","The Chariot"), "wheel":("La Rueda de la Fortuna","Wheel of Fortune"),
 "tower":("La Torre","The Tower"), "star":("La Estrella","The Star"), "sun":("El Sol","The Sun")}
tips = { # (activa, pasiva) es / en
 "fool":(("Activa: teletransporte seguro cercano o efecto de poción al azar.","Pasiva: caída lenta al caer."),
         ("Active: safe nearby teleport or a random potion effect.","Passive: slow fall when falling."))
,"magician":(("Activa: rayo arcano a distancia.","Pasiva: manos rápidas (Prisa)."),("Active: long-range arcane bolt.","Passive: Haste."))
,"empress":(("Activa: cura, hace crecer cultivos y calma animales.","Pasiva: regeneración y cultivos que crecen."),("Active: heals, grows crops, calms animals.","Passive: regeneration and crop growth."))
,"chariot":(("Activa: embestida que daña enemigos.","Pasiva: velocidad y salto."),("Active: damaging charge.","Passive: Speed and Jump Boost."))
,"wheel":(("Activa: 30s de suerte, fuerza y XP doble (20% de mala suerte).","Pasiva: suerte."),("Active: 30s of luck, strength and double XP (20% curse).","Passive: Luck."))
,"tower":(("Activa: rayo devastador donde mirás, sin destruir ítems.","Pasiva: resistencia en tormentas."),("Active: devastating lightning where you look, items untouched.","Passive: Resistance in storms."))
,"star":(("Activa: visión nocturna, regeneración II y purifica efectos negativos.","Pasiva: visión nocturna."),("Active: night vision, Regeneration II, cleanses negative effects.","Passive: Night Vision."))
,"sun":(("Activa: quema no-muertos cercanos y da resistencia al fuego.","Pasiva: resistencia al fuego."),("Active: burns nearby undead, grants fire resistance.","Passive: Fire Resistance."))}

def item_json(name):
    json.dump({"model":{"type":"minecraft:model","model":f"tarot:item/{name}"}}, open(f"{A}/items/{name}.json","w"), indent=2)
def model_json(name):
    json.dump({"parent":"minecraft:item/generated","textures":{"layer0":f"tarot:item/{name}"}}, open(f"{A}/models/item/{name}.json","w"), indent=2)

for cid, col in cards.items():
    n = f"card_{cid}"
    card_tex(f"{A}/textures/item/{n}.png", col, cid); item_json(n); model_json(n)

extras = {"destiny_deck":(90,40,140),"booster_pack":(60,60,160),"grimoire":(110,30,110)}
for n,c in extras.items():
    card_tex(f"{A}/textures/item/{n}.png", c, n); item_json(n); model_json(n)

# polvo estelar
T=(0,0,0,0); px=[[T]*16 for _ in range(16)]
for (x,y,c) in [(8,3,(255,255,200,255)),(7,4,(255,240,150,255)),(8,4,(255,255,255,255)),(9,4,(255,240,150,255)),(6,5,(220,190,255,255)),(7,5,(255,240,150,255)),(8,5,(255,255,200,255)),(9,5,(255,240,150,255)),(10,5,(220,190,255,255)),(7,6,(200,170,255,255)),(8,6,(230,210,255,255)),(9,6,(200,170,255,255)),(8,7,(180,150,240,255)),(5,9,(255,255,200,255)),(11,10,(255,255,200,255)),(4,11,(220,190,255,255)),(12,8,(220,190,255,255))]:
    px[y][x]=c
png(f"{A}/textures/item/stardust.png",16,16,px); item_json("stardust"); model_json("stardust")

# bloque
rnd = random.Random(7)
def block_tex(path, base, accent):
    px=[[tuple(base)+(255,) for _ in range(16)] for _ in range(16)]
    for y in range(16):
        for x in range(16):
            if x in (0,15) or y in (0,15): px[y][x]=tuple(accent)+(255,)
            elif rnd.random()<.08: px[y][x]=tuple(min(255,c+30) for c in base)+(255,)
    return px
top=block_tex(None,(70,35,110),(212,175,55))
for y in range(4,12):
    for x in range(4,12):
        if (x+y)%3==0: top[y][x]=(255,230,150,255)
png(f"{A}/textures/block/tarot_table_top.png",16,16,top)
png(f"{A}/textures/block/tarot_table_side.png",16,16,block_tex(None,(58,28,92),(212,175,55)))
png(f"{A}/textures/block/tarot_table_bottom.png",16,16,block_tex(None,(40,20,60),(120,100,40)))
json.dump({"parent":"minecraft:block/cube_bottom_top","textures":{"top":"tarot:block/tarot_table_top","side":"tarot:block/tarot_table_side","bottom":"tarot:block/tarot_table_bottom"}}, open(f"{A}/models/block/tarot_table.json","w"), indent=2)
json.dump({"variants":{"":{"model":"tarot:block/tarot_table"}}}, open(f"{A}/blockstates/tarot_table.json","w"), indent=2)
json.dump({"model":{"type":"minecraft:model","model":"tarot:block/tarot_table"}}, open(f"{A}/items/tarot_table.json","w"), indent=2)

# lang
def lang(idx_name, idx_tip, es):
    d = {"itemGroup.tarot":"Arcanos: Tarot" if es else "Arcana: Tarot",
         "block.tarot.tarot_table":"Mesa de Lectura del Tarot" if es else "Tarot Reading Table",
         "container.tarot.tarot_table":"Mesa de Lectura" if es else "Reading Table",
         "item.tarot.destiny_deck":"Mazo del Destino" if es else "Deck of Destiny",
         "item.tarot.booster_pack":"Sobre de Cartas" if es else "Card Booster Pack",
         "item.tarot.stardust":"Polvo Estelar" if es else "Stardust",
         "item.tarot.grimoire":"Grimorio Arcano" if es else "Arcane Grimoire"}
    for cid in cards:
        d[f"item.tarot.card_{cid}"] = names[cid][idx_name]
        d[f"tooltip.tarot.{cid}.active"] = tips[cid][idx_tip][0]
        d[f"tooltip.tarot.{cid}.passive"] = tips[cid][idx_tip][1]
    return d
for f,(i,t,es) in {"en_us":(1,1,False),"es_es":(0,0,True),"es_mx":(0,0,True),"es_ar":(0,0,True),"es_uy":(0,0,True)}.items():
    json.dump(lang(i,t,es), open(f"{A}/lang/{f}.json","w"), indent=2, ensure_ascii=False)

# datos
json.dump({"type":"minecraft:crafting_shapeless","category":"misc","ingredients":["minecraft:amethyst_shard","minecraft:glowstone_dust"],"result":{"id":"tarot:stardust","count":2}}, open(f"{D}/recipe/stardust.json","w"), indent=2)
json.dump({"type":"minecraft:crafting_shaped","category":"misc","pattern":["AGA","OBO","OOO"],"key":{"A":"minecraft:amethyst_shard","G":"minecraft:gold_ingot","O":"minecraft:obsidian","B":"minecraft:book"},"result":{"id":"tarot:tarot_table","count":1}}, open(f"{D}/recipe/tarot_table.json","w"), indent=2)
json.dump({"type":"minecraft:crafting_shapeless","category":"misc","ingredients":["minecraft:book","minecraft:amethyst_shard"],"result":{"id":"tarot:grimoire","count":1}}, open(f"{D}/recipe/grimoire.json","w"), indent=2)
json.dump({"type":"minecraft:crafting_shapeless","category":"misc","ingredients":["minecraft:paper","minecraft:paper","minecraft:paper","tarot:stardust"],"result":{"id":"tarot:booster_pack","count":1}}, open(f"{D}/recipe/booster_pack.json","w"), indent=2)
json.dump({"type":"minecraft:crafting_shaped","category":"misc","pattern":["PPP","PSP","PPP"],"key":{"P":"minecraft:paper","S":"tarot:stardust"},"result":{"id":"tarot:destiny_deck","count":1}}, open(f"{D}/recipe/destiny_deck.json","w"), indent=2)
json.dump({"type":"minecraft:block","random_sequence":"tarot:blocks/tarot_table","pools":[{"rolls":1,"bonus_rolls":0,"entries":[{"type":"minecraft:item","name":"tarot:tarot_table"}],"conditions":[{"condition":"minecraft:survives_explosion"}]}]}, open(f"{D}/loot_table/blocks/tarot_table.json","w"), indent=2)
json.dump({"replace":False,"values":["tarot:tarot_table"]}, open(f"{R}/data/minecraft/tags/block/mineable/axe.json","w"), indent=2)
print("assets ok")
