# Implementation Plan: Base visual e Inicio de TEO

**Git branch**: `001-inicio-base-visual`. El directorio `specs/001-inicio-base-visual` se mantiene. | **Date**: 2026-10-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-inicio-base-visual/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Rediseñar Inicio sobre la app Android existente y dejar lista la base visual para esa pantalla y para una lista mínima de avisos. Se conservan Kotlin, XML, ViewBinding, Fragments, ViewModels y Material Components. Los tokens de `docs/ui-ux/teo-ui-tokens.json` pasan a recursos `teo_` aplicados solo en esos dos destinos. El estado visible sale de la conexión actual y de `GET /api/status`, leyendo el despertar pendiente antes que el indicador de encendido. Los avisos pasan a conservar tipo, mensaje, fecha del evento y un momento de recepción, ordenados con la regla ya aclarada, sin un segundo consumidor de la cola.

## Technical Context

**Language/Version**: Kotlin y Java 11, en el módulo `ProyectoAndroid`. `minSdk` 24, `compileSdk` y `targetSdk` 36.

**Primary Dependencies**: AndroidX AppCompat, Navigation, Lifecycle con LiveData y ViewModel, Material Components y ViewBinding. No se agregan dependencias.

**Storage**: La lista de avisos sigue solo en memoria, con un tope de veinte. No hay historial en disco. El perfil, las rutinas y la agenda local no cambian.

**Testing**: JUnit en `app/src/test`. Compilación `assembleDebug`. La comparación visual es manual en el Samsung Galaxy S25 y en una ventana de 360 dp.

**Target Platform**: App Android para teléfono. Dispositivo principal de revisión: Samsung Galaxy S25. El espacio útil se registra en el momento de la prueba.

**Project Type**: Aplicación móvil Android dentro del repositorio existente. El servidor y el peluche quedan como contratos de consulta.

**Performance Goals**: No se fijan tiempos ni porcentajes de parecido. La espera de una acción se indica sin un porcentaje inventado y el control queda deshabilitado hasta el resultado.

**Constraints**: Tema claro solo en Inicio y en la lista nueva. Pantallas beta sin cambio global de tema. Primer uso y perfil intactos. Tiempo de uso, pulsador y agenda sin TEO sin cambio de comportamiento. No se modifican `ProyectoParaRasperrypiV5` ni la API.

**Scale/Scope**: Una pantalla rediseñada, una lista mínima nueva y la navegación ya acordada. El resto de las vistas queda para bloques posteriores.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principio | Resultado | Cómo se cumple |
|---|---|---|
| I. Conservar el funcionamiento existente | Pasa | Se reutilizan las operaciones actuales. No se edita el peluche ni el servidor. |
| II. Mantener la base técnica | Pasa | Siguen Kotlin, XML, ViewBinding, Fragments, ViewModels y Material. Sin dependencias nuevas. |
| III. Respetar las referencias visuales | Pasa | La guía, los tokens y el Inicio elegido mandan. La interfaz se arma con componentes, no con una captura. |
| IV. Conservar la identidad acordada | Pasa | Logo, pose saludando, variante de frente, confeti separado, cuatro secciones y nueve skins. |
| V. Mostrar datos y estados reales | Pasa | El modelo distingue confirmación, espera, ausencia y cero. HTTP exitoso no alcanza. |
| VI. Garantizar una interfaz adaptable | Pasa | dp, sp, áreas táctiles, insets y revisión en S25 y a 360 dp. |
| VII. Separar decisiones pendientes | Pasa | Tiempo, pulsador y agenda sin conexión no se resuelven. Inicio claro no cambia el tema beta. El modo noche del peluche no entra a Inicio. |
| VIII. Desarrollar por bloques | Pasa | Este plan cubre solo la base visual, Inicio y la navegación mínima. Configuración es destino temporal. |
| IX. Verificar antes de declarar terminado | Pasa | `quickstart.md` separa pruebas automáticas, captura y peluche. |

Revisión posterior al diseño: los contratos documentan rutas existentes y la interfaz visible. No aparecieron violaciones.

## Project Structure

### Documentation (this feature)

```text
specs/001-inicio-base-visual/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── existing-robot-api.md
│   └── home-ui.md
└── tasks.md              # lo crea /speckit-tasks, no este comando
```

### Source Code (repository root)

```text
ProyectoAndroid/app/src/main/
├── java/com/example/aplicacionparacelular/
│   ├── MainActivity.kt
│   ├── network/ParentInbox.kt
│   ├── network/RobotConnectionManager.kt
│   ├── ui/dashboard/DashboardFragment.kt
│   ├── ui/dashboard/DashboardViewModel.kt
│   └── ui/alerts/AlertsFragment.kt          # nuevo
├── res/
│   ├── values/colors.xml
│   ├── values/themes.xml
│   ├── values/dimens.xml
│   ├── values/strings.xml
│   ├── values-night/themes.xml
│   ├── layout/fragment_dashboard.xml
│   ├── layout/fragment_alerts.xml           # nuevo
│   ├── layout/item_parent_notice.xml        # nuevo
│   ├── layout/content_main.xml
│   ├── navigation/mobile_navigation.xml
│   ├── menu/bottom_navigation.xml
│   └── drawable/                            # logo, poses, confeti e iconos teo_
└── test/.../network/ParentInboxTest.kt
    test/.../ui/dashboard/                   # pruebas nuevas de estado y reproducción

docs/ui-ux/
├── 04-guia-visual-comun.md
├── teo-ui-tokens.json
└── referencias/logo-teo.png
    referencias/teo-saludando.png
    referencias/teo.png
```

**Structure Decision**: El trabajo queda dentro de `ProyectoAndroid/app`. No se crea otro módulo ni se toca el árbol del peluche. Las pantallas beta se reutilizan por sus destinos actuales: `nav_metrics`, `nav_stories`, `nav_routines`, `nav_medical` y `nav_config`.

## Archivos previstos

Se modifican:

- `fragment_dashboard.xml`, `DashboardFragment.kt` y `DashboardViewModel.kt`: composición nueva, estados, acciones, encuadre visible del personaje y un aviso destacado. El toque de Ver contenido, en `DashboardFragment`, abre `nav_stories`.
- `TeoCharacterFrame.kt`: cálculo del contenido opaco. `DashboardFragment` lo usa para presentar `teo-saludando.png`.
- `ParentInbox.kt`, `RobotConnectionManager.kt` y `ParentInboxTest.kt`: aviso estructurado, orden, tope y etiquetas. El poll sigue siendo el único lector de la cola.
- `colors.xml`, `themes.xml`, `dimens.xml`, `strings.xml` y `values-night/themes.xml`: recursos `teo_` y textos. La paleta lavanda permanece para el resto.
- `mobile_navigation.xml`: destino nuevo de avisos, creado en el mismo corte que su fragmento, su título, su regreso a Inicio y la referencia de `MainActivity`.
- `bottom_navigation.xml` y las cadenas de la barra: Inicio, Rutinas, Contenido y Agenda. Contenido apunta al destino de archivos. Agenda apunta al destino médico actual.
- `MainActivity.kt` y `content_main.xml`: ocultar la barra superior de la actividad en Inicio para no duplicar el encabezado y aplicar ahí el tema claro. `nav_alerts` se crea junto con su título, su regreso a Inicio, su tema claro y esa misma barra oculta. Entrar a un destino beta restaura la apariencia beta. Los insets de encabezado, desplazamiento y barra inferior se aplican en `MainActivity`, `DashboardFragment` y `AlertsFragment`, sin repetir el mismo margen. `OnboardingGate` y el recorrido de conexión y perfil no cambian.
- El menú overflow sigue abriendo Configuración en las pantallas beta. En Inicio, el engranaje del encabezado nuevo cumple ese destino temporal.

Se agregan:

- `AlertsFragment.kt` y `fragment_alerts.xml`: observan la lista retenida, muestran título y regreso a Inicio, y no llaman a la cola.
- `item_parent_notice.xml`.
- Recursos de logo, `teo_character_wave`, `teo_character_front`, fondo de celebración, campana y nota musical si no hay un vector compatible. El logo PNG transparente y `docs/ui-ux/referencias/fondo-logros.jpg`, actualizados por el usuario el 14 de octubre, sustituyen los recursos de preparación correspondientes.
- Pruebas de unidad del orden de avisos, del estado visible, de la etiqueta y la confirmación de reproducción, de la celebración y de la acción en curso.

No se modifican el primer uso, el perfil, Configuración, Actividad, Rutinas, Archivos ni Agenda, salvo el nombre visible Contenido y Agenda en la barra. Tampoco los proyectos del peluche. `teo.png` se incluye como recurso y no se usa para dormir o desconectar.

## Recursos visuales

Trasladar los colores, espacios, radios y tamaños de texto de `teo-ui-tokens.json` a recursos `teo_`. Los estilos de tarjeta, botón principal, botón secundario, estado y globo se definen aparte del tema de la app. Los botones `teo_` conservan las mayúsculas escritas y no activan la transformación automática de Material. `values-night` no oscurece esos recursos.

Inicio usa esa presentación clara antes de que exista la lista. La lista entra en el mismo tema claro en el corte que crea `nav_alerts`, con título, regreso a Inicio y sin la barra de actividad. Hasta ese corte ninguna tarea cita `nav_alerts`. Al entrar en una pantalla beta, `MainActivity` restaura la apariencia actual de barra, fondo y colores. El cajón lateral no se rediseña. En Inicio y avisos permanece cerrado y oculto, como ya ocurre durante el primer uso para otros motivos. Ese recorrido de primer uso no se altera.

El logo conserva sus proporciones. La pose saludando se copia sin recortar el archivo original. El encuadre toma el rectángulo de píxeles no transparentes, medido hoy de 371,34 a 927,738 dentro de 1365×768, y lo escala de forma uniforme hasta el tamaño visible del personaje en la tarjeta del boceto. `DashboardFragment` presenta `teo-saludando.png` con ese cálculo. Probar la función no alcanza si la vista no la usa. `fitCenter` no resuelve ese encuadre: escala también los márgenes transparentes y deja el oso más chico. La revisión en la tarjeta blanca mira bordes, orejas, brazos y que no aparezca un rectángulo negro. La pose de frente se copia igual, con su propio contenido visible de 312,28 a 1547,925 dentro de 1671×941, y no se usa para dormir o desconectar.

El confeti es un drawable detrás o al costado del texto, no un texto dibujado. Los iconos nuevos siguen el trazo de 24 dp de los vectores existentes. La campana no lleva contador.

## Estados y acciones

La derivación queda en una función pura, testeable sin Android, descrita en `data-model.md`.

- Desconectado: conexión Desconectado, actividad no disponible, acciones remotas deshabilitadas con una explicación. Detener visible y deshabilitado si no hay una reproducción vigente.
- `pending_wake`: Preparándose, aunque `power_on` venga verdadero.
- Modelos no listos y sin actividad despierta: Preparándose.
- Modelos listos, sin despertar pendiente y sin actividad despierta: Dormido y Despertar TEO.
- Despierto confirmado: Dormir TEO.
- Después de `POST /api/power`, leer `pending_wake`, `models_ready` y `power_on`. No copiar el valor que la app intentó enviar.
- Celebrar muestra «Celebración enviada a TEO» solo cuando el cuerpo confirma el envío. No anuncia un festejo terminado. Dormido, desconectado o no disponible se explican como condición.
- Con cuento activo, el botón dice «Volver a leer cuento». Sin él, «Reproducir música», con la última canción disponible o la primera. Catálogo vacío: mensaje hacia Contenido y sin reproducción anunciada.
- Reproducir y detener leen el cuerpo y se confirman con el estado posterior. Una solicitud rechazada, un error de consulta o un estado que no confirma no se anuncian como éxito ni borran lo conocido. Si se pierde la conexión, esa reproducción deja de presentarse como vigente.
- Detener permanece visible. Se habilita solo si el estado vigente trae canción o cuento.
- Interacciones muestran el número válido informado por la API, incluido cero. Una consulta fallida o un campo ausente o inválido se muestra como ausencia. No se promete separar un cero de respaldo de un cero registrado. La señal emocional no empieza en una emoción de ejemplo. Durante la carga, la acción en curso no se repite y no hay porcentaje. Al terminar en éxito o en error, los controles vuelven según la disponibilidad real de TEO.
- El anillo de tiempo y el control del pulsador salen de Inicio. Su comportamiento en Configuración y en el servidor no se cambia.

## Avisos

`ParentInbox` deja de ser el contrato visual de una línea con emoji. Pasa a ordenar avisos estructurados y a traducir el tipo a Logro, Pedido, Atención o Aviso. `RobotConnectionManager.pollNotifications` incorpora cada aviso una vez, con su `timestamp` si parsea y con el reloj de recepción de ese momento. Conserva veinte según el orden nuevo.

`parentAlerts` cambia de `List<String>` a la lista estructurada. El único observador actual es `DashboardFragment`. Ese observador se actualiza en el mismo corte que el tipo, para que el proyecto compile. La presentación final del aviso destacado es un corte posterior y no se mezcla con esa migración. La prueba `ParentInboxTest`, que hoy espera líneas con emoji, cambia con el modelo. `AlertsFragment` observa la misma lista y no llama a `getNotifications`. Las notificaciones del sistema siguen construyéndose con tipo y mensaje en `showSystemNotification`.

## Navegación

- Ver actividad: `nav_metrics`.
- Ver contenido: el toque en `DashboardFragment` abre `nav_stories`, mostrado como Contenido. Cambiar el texto y el menú no reemplaza ese evento.
- Mi TEO y Ver conexión: `nav_config`.
- Campana y Ver avisos: destino nuevo `nav_alerts`, creado en el mismo corte que su título, su regreso a `nav_dashboard` y su tema claro. Ningún corte anterior cita ese identificador.
- La barra conserva los cuatro destinos de nivel superior. Inicio sigue seleccionado en el tablero.
- `OnboardingGate` no se modifica. Wi-Fi, reintento y perfil siguen bloqueando el cromado hasta cumplir el recorrido actual.

## Adaptación y comprobación

Usar dp para layout y sp para texto. Botones de al menos 48 dp, sin altura fija para el cuerpo de las tarjetas. Si Reproducir y Detener no entran juntos con texto ampliado, pasan a columna y conservan la etiqueta. El desplazamiento de Inicio y de la lista incluye los insets superior e inferior, aplicados desde `MainActivity`, `DashboardFragment` y `AlertsFragment` además del XML. El mismo inset no se suma dos veces. No se reservan alturas copiadas de una captura.

Automático, sin teléfono ni peluche: las pruebas de unidad nuevas y las ya existentes que cubren primer uso y bandeja, más `assembleDebug`. Manual con teléfono: composición, modo oscuro, 360 dp, texto ampliado y barras del sistema. Manual con peluche: dormir, despertar pendiente, celebrar, música y cuento. El detalle está en `quickstart.md`.

## Complexity Tracking

No hay violaciones de la constitución que justificar. La lista nueva de avisos es la dependencia mínima ya aceptada, no una pantalla extra del rediseño completo.

## Bloqueos

No hay un bloqueo que impida implementar este plan. Hay una limitación que este bloque no resuelve: si falta el archivo de hoy, el servidor puede usar la telemetría en memoria; si no hay telemetría o el día no tiene archivo, puede enviar ceros de respaldo. La app muestra el número válido recibido. No distingue todos esos ceros de un cero registrado y no se agregará una API para hacerlo ahora. La ausencia queda reservada a un fallo o a un campo ausente o inválido.

La rama de trabajo es `001-inicio-base-visual`, creada desde `pc-pi-lan-discovery` sin descartar los cambios locales. El directorio `specs/001-inicio-base-visual` se mantiene.
