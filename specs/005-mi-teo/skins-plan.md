# Perfil y disfraces de TEO

Objetivo: corregir la presentación del perfil y elegir entre nueve disfraces con nombre e imagen. El personaje de la app usa el disfraz guardado; el logo conserva la identidad de la marca.

Composición: etiquetas legibles encima de los campos; opciones de género independientes que pueden pasar a otra fila; dos tarjetas blancas; selección de disfraz con miniatura, nombre y acceso a una cuadrícula de nueve imágenes. Mantener la paleta, tipografía y botones del Inicio aprobado.

Restricciones: Kotlin, XML y componentes nativos; conservar los nueve identificadores de la API, los límites de 40/600 caracteres y los PNG originales. La vista previa no escribe preferencias reales ni consulta a TEO. Producción cambia el personaje después de recibir un perfil completo confirmado y recuerda el último disfraz confirmado para el inicio sin conexión. Cancelar el selector conserva el borrador anterior.

- [x] Catálogo común de nueve parejas y encuadre uniforme según contenido visible. Probar identificadores, respaldo, límites y confirmación del perfil antes de implementarlos.
- [x] Selector visual y formulario compartido; conservar borradores y validación de primer uso y edición.
- [x] Aplicar el disfraz confirmado en Inicio y guardar por separado el estado simulado. Comprobar cancelación y restauración.
- [x] Ejecutar pruebas, compilar, instalar sin borrar datos y comparar Perfil, selector e Inicio en el S25. Registrar las comprobaciones reales y sus límites.

Archivos principales: `ui/character/TeoCostumeCatalog.kt`, `ui/character/TeoAppearance.kt`, `ui/character/TeoAppearanceRules.kt`, `ui/onboarding/CostumePicker.kt`, `ChildProfileForm.kt`, `include_child_profile_fields.xml`, `TeoCharacterPresenter.kt`, `RobotConnectionManager.kt`, `DashboardFragment.kt`, `MyTeoPreviewHost.kt` y `HomePreviewActivity.kt`.

Revisar especialmente: márgenes transparentes distintos entre PNG; textos ampliados; cancelar una selección; respuestas incompletas o fallidas; borrador que sobrevive a la recreación; aislamiento entre producción y vista previa. Sin cambios de rama ni commits en este trabajo.

Verificación: 163 pruebas unitarias sin fallos, APK compilado e instalado conservando los datos. Nueve miniaturas inspeccionadas en el S25; cancelar conserva Oso, guardar Tiburón lo muestra saludando en Inicio y reabrir Perfil conserva Tiburón. La recreación con escala de fuente 1,5 conserva nombre, género y disfraz; el selector pasa a dos columnas y permite desplazamiento. Escala restaurada a 1,0. Los 18 PNG conservan el hash del original y sus dimensiones y encuadres se comprobaron contra el canal alfa.

Revisión independiente: corregida la restauración de un formulario sin cambios mientras carga (se vuelve a consultar) y el caso de un intento de guardado posterior fallido (conserva la confirmación anterior). Las pruebas de concurrencia cubren consultas anteriores y simultáneas y respuestas de guardado fuera de orden. La persistencia real y el intercambio de perfil con el peluche requieren comprobación en el aparato; no se desplegó software en la Raspberry.
