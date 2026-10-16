# TEO: índice de bocetos de la app

Fecha: 13 de octubre de 2026. Etapa: referencias visuales previas a implementar el rediseño.

Hay imágenes de referencia para las veinte vistas P01–P20 del [inventario funcional](01-inventario-y-pantallas.md). Una vista puede tener varias imágenes, pasos o estados; el conteo no corresponde a veinte archivos ni a veinte pestañas. Inicio y Rutinas fueron elegidos explícitamente por el usuario; las demás imágenes se conservan como propuestas compartidas durante la revisión.

La [guía visual común v1](04-guia-visual-comun.md), su [muestra visual](guia-visual.html) y la [guía para Spec Kit y Cursor](05-speckit-y-cursor.md) reúnen la preparación para desarrollar. Sus tamaños y colores normalizados están propuestos para revisión antes de implementarlos.

## Referencias por vista

| Vista | Nombre | Boceto principal y variantes recibidas |
|---|---|---|
| P01 | Encontrar TEO | [Búsqueda sin resultado](bocetos/encontrar-teo-propuesta-2026-10-13.png) |
| P02 | Configurar Wi-Fi | [Verificar PIN](bocetos/wifi-pin-propuesta-2026-10-13.png) y [red con contraseña](bocetos/wifi-redes-propuesta-2026-10-13.png) |
| P03 | Perfil del niño | [Primer uso](bocetos/perfil-nino-propuesta-2026-10-13.png); la edición reutiliza los campos con regreso a Mi TEO |
| P04 | Inicio | [Principal elegido](bocetos/inicio-principal-elegido-2026-10-13.png), [dormido](bocetos/inicio-dormido-referencia-2026-10-13.jpg) y [desconectado](bocetos/inicio-desconectado-referencia-2026-10-13.png); [despierto anterior](bocetos/inicio-despierto-referencia-2026-10-13.jpg) como antecedente |
| P05 | Actividad | [Semana con datos](bocetos/actividad-propuesta-2026-10-13.png) |
| P06 | Avisos | [Lista reciente](bocetos/avisos-propuesta-2026-10-13.png) |
| P07 | Rutinas | [Lista elegida](bocetos/rutinas-elegida-2026-10-13.png) y [referencia del diálogo de eliminar](bocetos/rutinas-dialogo-eliminar-referencia-2026-10-13.png) |
| P08 | Crear o editar rutina | [Nueva rutina](bocetos/nueva-rutina-propuesta-2026-10-13.png); la edición reutiliza el formulario con datos existentes |
| P09 | Contenido: Música | [Catálogo y reproducción](bocetos/musica-propuesta-2026-10-13.png) |
| P10 | Contenido: Cuentos | [Catálogo](bocetos/cuentos-propuesta-2026-10-13.png) |
| P11 | Agregar música | [Estado de envío](bocetos/musica-envio-propuesta-2026-10-13.png); la selección usa el explorador nativo Android |
| P12 | Agregar cuento | [PDF y título](bocetos/agregar-cuento-propuesta-2026-10-13.png) |
| P13 | Detalle y edición de cuento | [Editar título y texto](bocetos/editar-cuento-propuesta-2026-10-13.png) |
| P14 | Agenda: Turnos | [Listado](bocetos/agenda-turnos-propuesta-2026-10-13.png) |
| P15 | Nuevo turno | [Formulario](bocetos/agenda-nuevo-turno-propuesta-2026-10-13.png) |
| P16 | Agenda: Vacunas | [Revisión del listado](bocetos/agenda-vacunas-revision-2026-10-13.png), con las correcciones funcionales anotadas debajo |
| P17 | Mi TEO | [Menú](bocetos/mi-teo-propuesta-2026-10-13.png) |
| P18 | Ajustes de TEO | [Volumen, brillo y modo noche](bocetos/ajustes-teo-propuesta-2026-10-13.png) |
| P19 | Conexión | [Estado y red actual](bocetos/conexion-propuesta-2026-10-13.png) |
| P20 | Diagnóstico avanzado | [Información y conexión manual](bocetos/diagnostico-avanzado-propuesta-2026-10-13.png) |

## Identidad y navegación que se conservan

Usar el [logo original](referencias/logo-teo.png) y los PNG individuales [TEO de frente](referencias/teo.png) y [TEO saludando](referencias/teo-saludando.png) como fuentes actuales de identidad. El usuario agregó ambos PNG con transparencia el 14 de octubre de 2026; la pose saludando coincide con el Inicio elegido. La [foto con skins](referencias/teo-y-skins.png) queda como antecedente, sin necesidad de recortar el personaje desde ella. Los logos redibujados por Gemini son referencias de ubicación. Se conservan el fondo marfil, el azul petróleo, las tarjetas blancas, el globo cian de interacciones y el confeti que el usuario pidió mantener.

La navegación principal aceptada tiene Inicio, Rutinas, Contenido y Agenda. Actividad y Avisos se abren desde Inicio; la campana también abre Avisos. El engranaje abre Mi TEO, con Ajustes, Conexión y Perfil. Diagnóstico avanzado se abre desde Conexión. Los formularios y las vistas secundarias usan sus recorridos de regreso; el primer uso mantiene las condiciones necesarias de conexión y perfil.

El usuario confirmó conservar las nueve skins actuales del código. El 15 de octubre agregó las nueve parejas originales en `referencias/skins` y pidió un selector con imagen y nombre y aplicar el disfraz guardado a las imágenes del personaje. Esa ampliación prevalece sobre el selector de texto del boceto. La pose de frente se usa en el selector y la pose saludando en Inicio; el logo oficial se conserva. Ver `specs/005-mi-teo/skins-plan.md`.

## Correcciones registradas para programar

- Unificar tipografía, iconos, colores, medidas y estados de los componentes; respetar los espacios para barras del sistema, teclado y desplazamiento. Los valores están propuestos en la guía visual común v1 y requieren revisión antes de implementarlos.
- Vacunas: una pendiente sin fecha muestra Sin fecha programada y Programar fecha; una pendiente con fecha muestra Cambiar fecha. La imagen de revisión omite el primer control y conserva un rótulo incorrecto en el segundo; prevalecen las reglas documentadas.
- Wi-Fi: agregar Redes disponibles sobre la lista. Conexión: dar menor protagonismo al acceso de diagnóstico. Avisos: quitar corchetes de etiquetas y conservar tipo y fecha de la API para mostrar horas reales.
- Actividad: distinguir ausencia de un total de vocabulario de un cero; corregir las etiquetas según el período elegido y el contraste del texto secundario. Las imágenes contienen datos ficticios, nunca valores a fijar en la app.
- Los botones deben reflejar conexión, sueño, carga, envío y resultado confirmado. Las variantes reutilizan los componentes; todavía no hay imágenes propias de todos los estados de carga, error, vacío, permisos y recuperación.

Las decisiones sobre tiempo de uso, despertar por pulsador y acceso a datos locales durante una desconexión siguen registradas en el inventario. Las referencias visuales no las dan por resueltas.

## Notas de cada grupo

- [Inicio: ficha y prompt original](02-inicio-ficha-y-prompt.md).
- [Rutinas: selección y reglas del formulario](bocetos/seleccion-rutinas.md).
- [Contenido: música y cuentos](bocetos/propuestas-contenido.md).
- [Agenda: turnos y vacunas](bocetos/propuestas-agenda.md).
- [Mi TEO: menú, ajustes, conexión y diagnóstico](bocetos/propuestas-mi-teo.md).
- [Primer uso: búsqueda, Wi-Fi y perfil](bocetos/propuestas-primer-uso.md).
- [Actividad y Avisos: datos y ajustes de presentación](bocetos/propuestas-actividad-y-avisos.md).

La guía visual común v1 ya está preparada para revisión. La guía de Spec Kit y Cursor propone organizar la especificación, el plan y las tareas del primer bloque sobre el código existente, y comparar Inicio en el Samsung Galaxy S25 antes de extender los componentes al resto. Esta colección no modifica el código de la app.
