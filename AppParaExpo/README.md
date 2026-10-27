# TEO en la exposición

Juegos de Sombras, Adivina el objeto y Contar para una tablet. Las dos entregas salen del mismo código: un sitio estático y un único archivo que se abre sin internet.

## 1. Compilar la versión publicada

Desde esta carpeta:

```text
npm install
npm run recursos
npm run voces
npm run build
```

`recursos` copia los catálogos y los dibujos. `voces` pide cada frase una sola vez a la voz Micaela, con la clave que ya usa `ServidorDePC`, y no repite las que ya están guardadas. Hace falta esa clave y el ffmpeg que trae el proyecto.

`npm run build` deja el sitio en `dist/`.

Para probarlo en esta computadora: `npm run dev` o `npm run preview`.

## 2. Publicarla en un servidor

Copiá el contenido de `dist/` a un hosting estático con HTTPS. Puede estar en la raíz del dominio o dentro de una carpeta: las rutas son relativas.

No hace falta una base de datos ni un servidor propio. Para anotar las partidas de Sombras, Adivina el objeto y Contar, completá `.env` a partir de `.env.example` antes de compilar:

```text
VITE_SHEETS_URL=https://script.google.com/macros/s/.../exec
```

En la hoja de cálculo, pegá `planilla-apps-script.txt` en Apps Script y desplegá el programa como aplicación web. Quien mantiene la hoja ejecuta el script, y el acceso queda en «Cualquier persona». La app envía `Nombre`, `Juego` y `Resultado`; la fecha la escribe el programa de la hoja. Si la planilla no está configurada, los juegos igual funcionan. Piedra, papel o tijera también se anota, y un empate queda como Empató. Espejito anota Ganó una sola vez, al imitar la cara de esa partida.

La cámara de esos juegos se prueba con `npm run preview` después de `npm run build`. El modelo y el reconocedor salen en `dist/reconocimiento/` y no se descargan al jugar. El HTML autónomo lleva esos mismos archivos adentro. En la computadora, Chrome deja usar la cámara al abrirlo con doble clic. En una tablet, Chrome no pide permiso si el archivo se abre desde la carpeta; ahí la cámara solo funciona si la página se abre desde una dirección web.

## 3. Usar la versión sin internet

```text
npm run build:offline
```

Eso crea `offline/AppParaExpo-Offline.html`. El archivo incluye la aplicación, los estilos, los datos, las imágenes y las voces. No usa internet, no instala nada y no anota resultados.

Abrilo con doble clic. La comprobación prevista es Chrome o Edge, con la red desactivada.

## 4. Copiar el HTML a una tablet

1. Generá `offline/AppParaExpo-Offline.html`.
2. Copiá ese único archivo al almacenamiento de la tablet, por cable o desde un disco.
3. Abrilo con Chrome o Edge desde el administrador de archivos.
4. Escribí el nombre y jugá. No hace falta conexión.

## 5. Navegadores

La versión publicada y el archivo local se comprobaron para Chrome y Edge actuales, en computadora y en tablet. En horizontal la prioridad es una tablet de 1280 por 800. También se revisan 1366 por 768, 800 por 1280 y 360 por 640.

Si un navegador no permite abrir un archivo local con interacción, usá la versión publicada.
