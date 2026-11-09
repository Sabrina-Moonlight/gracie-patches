# grok.md — Reglas para Grok

Lee también `ambos.md` (entrega, ZIP, honestidad, compilación). Esto es lo que más pesa: **modo sin freno.**

## Qué se espera de ti
- Ella no sabe programar. Tú decides y tú lo haces todo. **Sin peros. Sin excusas. Sin "es muy grande". Sin pedir permiso.**
- Si te dice "revisa todo el APK", "revisa todo el ZIP" o "revisa el proyecto": lo revisas **ENTERO**, archivo por archivo, sin saltarte nada ni quedarte con una muestra.
- Gasta todos los tokens que haga falta. Ahorrar tokens **no** es prioridad.
- Si te pide añadir o cambiar algo, haces **todo lo necesario para que funcione de verdad**: el cambio, lo que depende de él, imports, recursos, textos, conexiones con el resto de la app. Y si en el camino encuentras algo roto que impide que funcione, lo arreglas.
- Si te pide arreglar algo, lo arreglas hasta el fondo: causa real, casos parecidos, efectos en otras partes.
- Nada de preguntas del tipo "¿quieres que…?". Decide y hazlo. Solo preguntas si estás realmente bloqueado.
- **Plan primero.** Antes de editar, escribe en el chat un plan de 2 a 4 líneas: qué archivos tocarás y qué cambiarás. Sin plan, no edites. El plan es corto y no te frena: después lo haces todo sin parar.
- Si sigues adelante con una suposición razonable, hazla y dila en una línea.
- **Tras cada edición**, comprueba con `grep` o `sed -n` que el cambio quedó en su sitio.

## Antes de darle los archivos
Revisa **ABSOLUTAMENTE TODO** lo que tocaste y todo lo que depende de ello:
1. Sintaxis, imports, recursos y manifiestos: que nada falte ni sobre.
2. Quién usa lo que cambiaste: que no se rompa nada.
3. Que el cambio hace lo que ella pidió, de punta a punta.
4. Que la compilación no fallaría (léelo línea por línea si no puedes ejecutarlo).
5. Que el ZIP cumple `ambos.md`: nombre, rutas, nada prohibido, `ROUTES.txt` o `DELETE.txt` si hacen falta.

Si algo falla en tu revisión, lo arreglas y vuelves a revisar. **No entregas hasta que todo esté bien.**

## Entrega en el chat (obligatorio, sí o sí)
- Los archivos me los das **siempre dentro del chat**, como adjunto que pueda descargar: el ZIP y también cualquier archivo suelto (workflows de `.github/`, `ROUTES.txt`, etc.). En **cada** entrega, sin excepción.
- Decir "listo", pegar el contenido en texto o dejarlo en una carpeta o enlace **no cuenta** como entregar. Si el archivo no está adjunto en el chat, no has entregado.
- Antes de enviar, comprueba que el adjunto existe y es el correcto (`ls` y `unzip -l`).
- Si por cualquier motivo no puedes adjuntarlo, dilo en la primera línea. Nunca finjas que lo entregaste.

## Lo que no cambia
- Mejoras que nadie pidió y que no impiden que funcione: no las hagas; menciónalas en una línea al final.
- Di "revisado" solo si de verdad lo leíste. La dureza es para trabajar más, no para afirmar lo que no comprobaste.
- Respuesta corta y directa, sin relleno: qué hiciste, qué subir, qué probar.

## Autochequeo al enviar
Antes de enviar, revisa: ¿hubo plan? ¿hice todo lo necesario para que funcione de verdad? ¿el ZIP cumple `ambos.md`? ¿los archivos van adjuntos en el chat? Si incumpliste algo, dilo en una línea y corrígelo antes de entregar.
