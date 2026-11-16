# Arcanos: Mod del Tarot (Fabric 1.21.11)

## Compilar
Requiere JDK 21 y Gradle 9.2+ (o generá el wrapper con `gradle wrapper`).
    gradle build          # jar en build/libs/
    gradle runClient      # probar en el juego

## Estructura
- card/      TarotCard (datos), TarotCards (registro: 8 cartas), CardAbilities (activas + pasivas)
- item/      TarotCardItem, DestinyDeckItem (Mazo), BoosterPackItem, GrimoireItem, ModItems, ModComponents (nivel de carta)
- block/     TarotTableBlock (Mesa de Lectura), ModBlocks
- screen/    TarotTableScreenHandler (lectura + fusión), ModScreens
- prophecy/  ProphecyManager (Fortuna / Peligro / Encuentro), TraderOffers
- client/    TarotClient, TarotTableScreen (GUI)

## Cómo se juega
- Carta en la mano: clic derecho = activa. Carta en la mano secundaria = pasiva (cada 1 s).
- Mazo: clic = robar · shift+clic con carta en la otra mano = guardarla.
- Mesa: 3 cartas + 1 amatista (o 5 niveles) -> "Leer futuro" -> profecía en 1-10 min.
- Fusión: 2 cartas iguales del mismo nivel + Polvo Estelar (2 para Astral, 4 para Cósmica).
- Recetas: Polvo Estelar (amatista + polvo de piedra luminosa), Mesa, Grimorio, Sobre, Mazo.

## Agregar una carta
1. Una línea en TarotCards.java + sus dos métodos en CardAbilities.java.
2. Agregarla en tarot-mod/gen_assets (textura, lang) o a mano.

## Pendiente / conocido
- Mago: el descuento de XP al encantar necesita un mixin (hoy da Prisa).
- Rueda: duplica XP, no el loot (necesita mixin/accessor de dropLoot).
- Profecías en memoria: se pierden al reiniciar el servidor.
- Falta: Arcanos restantes, Menores, jefes, Altar, estructuras, Vidente aldeano, expansiones.
