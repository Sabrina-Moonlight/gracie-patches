# AGENTS.md

## Protocolo (en este orden)
2. Antes de la primera edición, escribe en el chat un plan de 3 a 5 líneas: qué archivos tocarás y qué cambiarás. Si no está en el chat, no empieces.
3. Lee lo mínimo del **proyecto de patches**: `grep` y rangos de líneas. No listes ni abras el repo entero. No releas lo ya leído.
4. Haz lo pedido **y todo lo necesario para que funcione de verdad**. Si el objetivo es quitar anuncios, unlock, pairip, etc., analiza la APK completa (smali/clases reales, no solo un SDK) antes de editar. No te quedes en el primer corte superficial.
5. Tras cada edición, comprueba con `grep` o `sed -n` que quedó en el sitio correcto.
6. Agrupa ediciones relacionadas.
7. Pregunta solo si te bloquea. Si puedes seguir con una suposición razonable, hazla y dila en una línea.

Antes de responder revisa: ¿seguiste el plan? ¿el arreglo cubre la causa real, no solo un síntoma? ¿entregas en el formato de "Entrega"? ¿la respuesta es corta (qué hiciste, qué subir, qué probar)? Si incumpliste una regla, dilo en una línea y corrígelo.

## Análisis de APK (obligatorio cuando el pedido lo requiere)
- Si pide quitar anuncios, unlock, licencia, pairip, o “revisa toda la APK”: **desensambla y revisa el APK completo** (clases de la app + librerías de ads/license) **antes** de la primera edición de patches.
- Busca interruptores maestros, implementaciones reales (no solo el SDK de mediación), diálogos, botones y preferencias que controlen el comportamiento.
- No respondas con un parche a medias. Entrega el corte que cubra todas las rutas encontradas, o di en una línea qué ruta no pudiste confirmar.
- Confirma en el chat, en pocas palabras, qué clases/métodos reales tocaste.

## Prioridades
1. Que el objetivo funcione de punta a punta.
2. Sin extras ni refactors no relacionados.
3. Tono directo: sin relleno, sin halagos, sin disculpas innecesarias. Sin gritar ni mayúsculas innecesarias.

## Honestidad
- No afirmes que algo funciona si no lo comprobaste en el código.
- Si no estás seguro de la causa, dilo y propón la forma más barata de confirmarla.
- No inventes archivos, funciones ni datos. Si no está en el código, dilo.

## Sobre ella
- No sabe programar. Habla **siempre en español**, con palabras sencillas y sin jerga.
- Ya conoce varias cosas de GitHub. No le expliques lo básico (subir archivos, ejecutar workflows, fusionar pull requests). Si necesita hacer algo nuevo a mano, dale los pasos exactos, uno por uno.
- Tú modificas el código y las aplicaciones. Tienes **total libertad** y permiso total para leer, crear, modificar y borrar archivos del proyecto. No preguntes "¿puedo hacer X?": hazlo y explica después.
- Ella es la tester: sube, compila, prueba y te cuenta.
- No tienes acceso a su GitHub. Nunca digas "no puedo compilar ni probar": entrega, y espera a que ella lo haga y te cuente.
- Cada chat empieza sin recordar nada: este archivo es la memoria. Si cambia una regla de trabajo, actualízalo.

## Entrega
Entrega siempre los cambios en **un ZIP**. El workflow **Sort Uploads** lo coloca solo en el proyecto al subirlo.

Dentro del ZIP:
- Solo los archivos que cambiaste, con su nombre original. Una línea en el chat de qué cambió en cada uno.
- `ROUTES.txt` si un archivo es nuevo o su nombre existe en varias carpetas: una línea por archivo, `Nombre.ext = ruta/completa/Nombre.ext`.
- `DELETE.txt` si hay que borrar archivos: una ruta completa por línea. Solo archivos, nunca carpetas, y nunca `LICENSE`, `.git` ni `.github`.

Fuera del ZIP, como archivo suelto: todo lo que Sort Uploads no puede tocar, como los archivos de `.github/`. Dile que lo edite a mano en GitHub.

**AGENTS.md** también va en el ZIP cuando lo actualices (ruta raíz: `AGENTS.md`).

## Compilación
- Normalmente ella sube los archivos, el bot los ordena solo y también lanza el release.
- No prepares ni compiles nada por tu cuenta, salvo que ella lo pida.
- Antes de dar algo por listo, revisa leyendo el código que no rompa la compilación (sintaxis, imports, recursos que falten).
- Si la compilación falla, pídele que copie el mensaje de la línea marcada en rojo y arregla en un cambio nuevo.
