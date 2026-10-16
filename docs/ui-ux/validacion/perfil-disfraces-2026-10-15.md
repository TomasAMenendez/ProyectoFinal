# Perfil y disfraces — Samsung Galaxy S25

Android 16; pantalla física 1080 × 2340, densidad física 480 y override 420 (ancho lógico aproximado 411 dp). Navegación por botones. Fuente habitual 1,0; prueba temporal 1,5, restaurada a 1,0. La revisión usa TEO Vista previa y los mismos layouts que producción.

Comprobado:

- Formulario con etiquetas y contadores legibles; tres opciones de género completas y alineadas; campos de varias líneas y desplazamiento.
- Nueve disfraces con nombre e imagen de frente, resaltado y encuadre uniforme.
- Elegir Tiburón y cancelar conserva Oso. Confirmar lo incorpora al borrador; guardar un perfil válido muestra Tiburón saludando en Inicio.
- Reabrir Perfil conserva Tiburón. Recrear la actividad con texto ampliado conserva nombre, género y disfraz; el selector usa dos columnas y desplazamiento.
- 163 pruebas unitarias, sin fallos; `:app:testDebugUnitTest :app:assembleDebug` correcto. APK instalado conservando los datos.
- Los 18 recursos PNG son idénticos a los originales; dimensiones y rectángulos visibles verificados contra el canal alfa, ignorando píxeles con alfa de 8 o menos para el encuadre.

Capturas:

- `s25-perfil-disfraces-2026-10-15.png`
- `s25-perfil-disfraz-seleccionado-2026-10-15.png`
- `s25-selector-disfraces-2026-10-15.png`
- `s25-perfil-tiburon-2026-10-15.png`
- `s25-inicio-tiburon-2026-10-15.png`
- `s25-perfil-texto-ampliado-2026-10-15.png`
- `s25-disfraces-texto-ampliado-2026-10-15.png`

La integración real usa los GET/PUT existentes de perfil y recuerda el último disfraz confirmado. Las reglas de confirmación y concurrencia están cubiertas por pruebas de unidad. No se comprobó ese intercambio ni la persistencia real con el peluche físico. La vista previa no consulta ni escribe en TEO ni en las preferencias reales. No se desplegó software en la Raspberry. No se repitió aquí toda la validación de las otras pantallas de Mi TEO ni de modo oscuro o ventana de 360 dp.

Corrección posterior: el perfil nuevo de vista previa comienza vacío. Nombre muestra «Ej.: Luna» y Qué le gusta muestra «Animales, música, cuentos…» como sugerencias, con contadores 0/40 y 0/600. Escribir Ana sin borrar la sugerencia produce únicamente Ana. Comprobado en el S25, captura `s25-perfil-sugerencias-2026-10-15.png`; compilación y las siete pruebas de perfil y borrador pasan. Los datos guardados siguen precargándose al editar.
