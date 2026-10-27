# Research: Piedra, papel o tijera en la exposición

Fecha: 2026-10-26. No quedan ítems `NEEDS CLARIFICATION`.

## Dónde vive el juego

- **Decision**: Ampliar `AppParaExpo`. `JuegoId` suma `ppt`. El menú agrega la tarjeta con `Imagen_Tijera.png`. `Aplicacion` abre `PantallaPpt` y no llama a `registrarResultado` para este juego.
- **Rationale**: El menú, el marco, los botones y las dos compilaciones ya existen. Un proyecto nuevo duplicaría la navegación y las voces. La planilla actual solo entiende ganó o perdió de los tres juegos; el empate no entra en esa fila.
- **Alternatives considered**: Anotar victoria y derrota y omitir el empate dejaría la planilla incompleta sin que el pedido lo pidiera. Crear otra carpeta de aplicación rompe la consigna de no reconstruir la web.

## Imágenes originales

- **Decision**: Copiar, sin editar el origen, `ProyectoParaRasperrypiV5/juegos/media/Imagen_Piedra.png`, `Imagen_Papel.png` e `Imagen_Tijera.png` hacia `AppParaExpo/src/recursos/imagenes/`. No ejecutar `scripts/copiar-recursos.mjs`.
- **Rationale**: Esos tres nombres están en la carpeta de medios. El resto de las imágenes de la exposición es más nuevo que el origen; el script de copia las pisaría. El menú ya arma la dirección de un PNG que está en esa carpeta.
- **Alternatives considered**: Guardarlas solo dentro de `src/juegos/ppt` obligaría a otro cargador distinto del menú. Generar figuras nuevas contradice el pedido.

## Reconocedor

- **Decision**: `@mediapipe/tasks-vision`, modo `VIDEO`, `recognizeForVideo`, `numHands: 1`. Puño `Closed_Fist` es piedra, palma `Open_Palm` es papel y `Victory` es tijera. La comparación ignora mayúsculas. El delegado intenta GPU y, si no puede, CPU. El módulo se importa solo al tocar «Activar cámara».
- **Rationale**: Es el reconocedor preentrenado que el pedido nombra. Esas tres categorías son las que el modelo publica. Cargarlo al entrar al menú pediría trabajo y permiso antes de tiempo.
- **Alternatives considered**: Comparar píxeles o reglas de dedos queda fuera del pedido. Reconocer cara o cuerpo no aporta a la jugada.

## Modelo y WASM locales

- **Decision**: Instalar el paquete con npm. Copiar su carpeta `wasm` a `AppParaExpo/reconocimiento/wasm`. Guardar una sola vez `gesture_recognizer.task` en `AppParaExpo/reconocimiento/`. En la compilación publicada, `FilesetResolver.forVisionTasks` apunta a `./reconocimiento/wasm` y el modelo a `./reconocimiento/gesture_recognizer.task`, con `base: './'`. Un script de copia los lleva a `dist/reconocimiento` y a `offline/reconocimiento`. Durante la partida no hay solicitudes a `storage.googleapis.com` ni a un CDN.
- **Rationale**: El cargador oficial pide una carpeta de WASM y una ruta de modelo. Esas rutas relativas funcionan en `dist/` servido por HTTPS o por `vite preview`. El archivo del modelo pesa varios megabytes y no debe volver a descargarse en la exposición.
- **Alternatives considered**: Dejar la URL de Google cumple el primer arranque con internet y falla la consigna offline. Incrustar el modelo en el HTML único agranda el archivo que ya pesa unos 7 MB y igual no resuelve el WASM.

## HTML autónomo y la cámara

- **Decision**: `npm run build:offline` sigue produciendo `offline/AppParaExpo-Offline.html` para Sombras, Adivina, Contar y la partida por figuras. La cámara no se promete al abrir ese archivo con doble clic. Chrome bloquea `fetch` de archivos vecinos desde `file://`, y MediaPipe necesita ese `fetch` para el WASM. Además, `getUserMedia` en `file://` no es una prueba confiable. La prueba de cámara es `npm run build` y `npm run preview`, con la red cortada después de compilar. Si hace falta la tablet sin un servidor público, se sirve la carpeta `dist/` en la red local: los archivos ya viajan con la app.
- **Rationale**: El pedido permite conservar el HTML único e informar el límite, sin agregar un CDN. Los botones de las figuras no necesitan el modelo.
- **Alternatives considered**: Un Worker o un blob único no cambia el hecho de que el cargador oficial busca varios archivos WASM por ruta. Obligar a Chrome a abrir `file://` con un flag no es una entrega para la exposición.

## Hilo principal, sin Worker

- **Decision**: El análisis corre en el hilo de la página, con un reloj de cerca de 100 ms. Si una inferencia sigue en curso, el tick se salta. No se usa Web Worker.
- **Rationale**: `recognizeForVideo` lee el elemento de video de la página. Pasar ese cuadro a un Worker exige otra tubería y más copias. Diez inferencias por segundo alcanzan para juntar 800 ms con varias observaciones y dejan la pantalla capaz de recibir toques.
- **Alternatives considered**: Analizar cada fotograma de la cámara carga de más una tablet. Un Worker solo conviene si el modelo aceptara un bitmap ya copiado; este modo de video no está armado así.

## Estabilidad, cuenta y espejo

- **Decision**: Funciones puras. Cada observación con confianza mayor o igual a 0,7 de la misma clase suma tiempo. Otra clase, una mano ausente o una confianza menor reinicia el acumulado. A los 800 ms empieza la cuenta. Los números 3, 2 y 1 duran cerca de un segundo cada uno y se cancelan si la clase confirmada no sigue presente. El video se refleja solo con CSS. A `recognizeForVideo` se le pasa el video sin invertir.
- **Rationale**: Así una detección suelta no juega, y el espejo no cambia un puño por otra clase. La lógica se prueba sin cámara.
- **Alternatives considered**: Aceptar la primera categoría que supere 0,5 dispara partidas por un fotograma borroso. Invertir el canvas antes de clasificar no está pedido y puede empeorar el modelo.

## Jugada de la computadora y botones

- **Decision**: `elegirComputadora(azar)` devuelve piedra, papel o tijera con el generador inyectado, y solo se llama cuando la jugada de la persona ya quedó fija. Tocar una figura fija esa jugada en el acto, cancela la cuenta y recién ahí llama al azar.
- **Rationale**: El pedido prohíbe que la computadora elija antes. El toque es una decisión explícita y no necesita los 800 ms. El generador inyectado permite afirmar que las tres figuras salen con la misma función.
- **Alternatives considered**: Elegir la jugada de la computadora al entrar a la ronda permitiría sesgo o anticipación, aunque no se mostrara.

## Permiso, errores y apagado

- **Decision**: `getUserMedia` con `audio: false` y `facingMode: { ideal: 'user' }`, tamaño ideal 640×480 y sin mínimo obligatorio. Al desmontar la pantalla o volver al menú se hace `stop()` en cada pista, se corta el bucle y se llama a `close()` del reconocedor. Un fallo de permiso, de dispositivo o de modelo muestra el mensaje y deja las tres figuras usables.
- **Rationale**: La especificación pide no dejar la cámara encendida y no exigir una resolución que deje afuera a un equipo chico. Una sola instancia evita reconocedores duplicados si React vuelve a montar la pantalla en desarrollo.
- **Alternatives considered**: Pedir 1280×720 como mínimo deja sin juego a cámaras modestas. Crear el reconocedor en el menú mantendría el modelo cargado fuera de la partida.
