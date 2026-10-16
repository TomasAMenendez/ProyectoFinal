# Feature Specification: Rutinas

**Feature Branch**: `001-inicio-base-visual`

**Created**: 2026-10-14

**Status**: Draft

**Input**: User description: "Módulo de Rutinas: listado, alta, edición y confirmación para eliminar, con vista previa de depuración aislada."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver y activar rutinas (Priority: P1)

El adulto abre Rutinas y ve cada recordatorio con la hora destacada, el nombre, el mensaje, el interruptor y si está activada o desactivada. Puede activar o desactivar una rutina sin entrar al formulario.

**Why this priority**: Sin el listado no hay módulo.

**Independent Test**: Con rutinas conocidas, la pantalla muestra esa distribución y el interruptor cambia solo la activación.

**Acceptance Scenarios**:

1. **Given** rutinas guardadas, **When** se abre Rutinas, **Then** cada una muestra hora, nombre, mensaje, interruptor y Activada o Desactivada, con Editar y Eliminar visibles.
2. **Given** una rutina con completado o recordatorio indicado por los datos, **When** se muestra, **Then** aparece Completada o Recordada. Si el dato no existe, esa marca no aparece.
3. **Given** ninguna rutina, **When** se abre la sección, **Then** se puede agregar una y no se inventa un estado de sincronización.

---

### User Story 2 - Crear una rutina (Priority: P1)

El adulto abre un formulario de pantalla completa, indica nombre, hora y mensaje, y guarda. La rutina queda activada. Si el mensaje está vacío, se usa la frase automática ya existente.

**Why this priority**: El alta es el camino para tener recordatorios.

**Independent Test**: Guardar con nombre y hora crea una rutina activada; un nombre vacío no descarta lo escrito.

**Acceptance Scenarios**:

1. **Given** el formulario de alta, **When** el nombre está vacío y se guarda, **Then** se señala el nombre y permanecen hora y mensaje.
2. **Given** un nombre y un mensaje vacío, **When** se guarda, **Then** la rutina usa la frase automática y queda activada.
3. **Given** un fallo al guardar, **When** vuelve la respuesta, **Then** el formulario conserva los datos y muestra el error.

---

### User Story 3 - Editar una rutina (Priority: P2)

Editar abre el mismo formulario con los datos cargados, título Editar rutina y la acción Guardar cambios. No dice que la rutina se agregará activada. Conserva la activación y los campos que no se cambian.

**Why this priority**: Corrige un recordatorio sin recrearlo.

**Independent Test**: Abrir una rutina desactivada, cambiar solo el nombre y guardar deja la hora, el mensaje y la desactivación iguales.

**Acceptance Scenarios**:

1. **Given** una rutina existente, **When** se elige Editar, **Then** nombre, hora y mensaje aparecen precargados.
2. **Given** una rutina desactivada, **When** se guarda un cambio de nombre, **Then** sigue desactivada.

---

### User Story 4 - Eliminar con confirmación (Priority: P2)

Eliminar pregunta en un diálogo centrado, con el nombre de la rutina, sobre el listado atenuado. Cancelar no borra. Eliminar quita esa rutina.

**Why this priority**: Evita borrar un recordatorio por un toque accidental.

**Independent Test**: Cancelar conserva la lista; confirmar quita solo la rutina nombrada.

**Acceptance Scenarios**:

1. **Given** el listado, **When** se elige Eliminar, **Then** el diálogo nombra esa rutina y ofrece Cancelar y Eliminar.
2. **Given** el diálogo abierto, **When** se cancela, **Then** la rutina sigue en la lista.

---

### Edge Cases

- El mensaje de alta puede ocupar varias líneas. Un mensaje vacío de edición conserva el mensaje anterior.
- Si la hora no es válida, no se guarda y los demás campos siguen escritos.
- Completada tiene prioridad visual sobre Recordada cuando ambos datos vienen juntos.
- La vista previa de depuración recorre listado, alta, edición y eliminación sin consultar al peluche, sin programar recordatorios y sin escribir el almacenamiento real.
- El primer uso de la app real no se omite.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El listado MUST usar la distribución hora | nombre y mensaje | interruptor y estado, con Editar y Eliminar al pie.
- **FR-002**: El texto de apoyo MUST ser «Organizá recordatorios para acompañar su día». MUST NOT mostrarse un estado de sincronización inventado.
- **FR-003**: Completada y Recordada MUST aparecer solo cuando los datos de la rutina lo indiquen.
- **FR-004**: Alta y edición MUST ser una pantalla completa con regreso, no el diálogo anterior.
- **FR-005**: El nombre MUST ser obligatorio. La validación o un guardado fallido MUST conservar lo escrito y mostrar el error.
- **FR-006**: Una rutina nueva MUST crearse activada. El mensaje vacío de alta MUST usar la frase automática existente.
- **FR-007**: Editar MUST precargar los datos, conservar la activación y los campos no modificados, y MUST NOT repetir la aclaración de alta.
- **FR-008**: Eliminar MUST pedir confirmación con el nombre, sobre el listado atenuado.
- **FR-009**: Los contratos de rutinas, los recordatorios y el peluche MUST conservarse. Anticipación y mensajes de transición no se agregan como opciones.
- **FR-010**: La compilación de depuración MUST ofrecer un recorrido de ejemplo aislado. La compilación release MUST NOT incluir esos ejemplos.

### Key Entities

- **Rutina**: identidad, nombre, hora, mensaje, activación y, si el sistema los informa, marcas de recordada y completada.
- **Borrador**: nombre, hora y mensaje escritos antes de confirmar el guardado.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En una revisión visual, las tres zonas de cada tarjeta coinciden con el listado elegido y los dos accesos del pie quedan visibles sin abrir otro menú.
- **SC-002**: Un alta con nombre vacío no pierde el texto ya escrito y no agrega una rutina.
- **SC-003**: Editar una rutina desactivada y guardar un solo campo la deja desactivada.
- **SC-004**: Cancelar la eliminación deja la misma cantidad de rutinas; confirmar la reduce en una.
- **SC-005**: La vista previa permite recorrer las cuatro pantallas sin un peluche conectado.

## Assumptions

- La frase automática existente es «¡Es hora de » más el nombre en minúsculas.
- Las marcas recordada y completada llegan con la consulta de rutinas; no se calculan en la pantalla.
- El fondo del boceto de eliminación no se copia: el diálogo se apoya en el listado elegido.
- Inicio permanece como está aprobado. Rutinas reutiliza su identidad y define su propia composición.

## Ampliación: pictograma opcional

- Nueva rutina y Editar rutina ofrecen «Pictograma en TEO (opcional)». El alta empieza en Sin pictograma. El selector nativo muestra Baño, Cepillar los dientes, Comer, Jardín, Médico y Sin pictograma, con la miniatura del PNG original y la selección visible. Cancelar no cambia el valor. Editar precarga el id guardado. Un error o una recreación del formulario no lo pierde. Elegir una imagen no llama al peluche hasta guardar.
- Los ids son `banio`, `cepillar_dientes`, `comer`, `jardin` y `medico`. El campo `pictogram_id` es opcional en GET y POST `/api/routines`. Ausente, null o vacío significa sin pictograma. No se infiere por el nombre. Crear, editar, activar, desactivar y eliminar conservan los pictogramas del resto del lote. Sin pictograma lo borra. Un id desconocido o un archivo ausente no impide el audio ni cierra el proceso.
- El recordatorio principal con id válido muestra ese PNG a pantalla completa, centrado, sin deformarlo ni recortarlo, durante ocho segundos desde el inicio real de su audio, y después vuelve a los ojos. El preaviso no muestra pictograma. La imagen viaja con el id de rutina, el tipo de evento y el mensaje hasta la reproducción; no se elige por el texto ni por la primera rutina pendiente. Un temporizador viejo no apaga la imagen de un evento posterior. Sueño, PIN y el pictograma de abrazo conservan su prioridad.
- La vista previa recorre rutinas con y sin pictograma usando el mismo catálogo y los mismos layouts, sin API ni almacenamiento real.
- El trabajo continúa en la rama actual, sin commit ni cambio de rama. Las verificaciones pendientes del primer bloque siguen pendientes.
