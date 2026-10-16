# Contract: Pantallas de Rutinas

- Listado: `fragment_routines.xml` + `item_routine.xml`.
- Formulario: `fragment_routine_editor.xml` para alta y edición.
- Diálogo: `dialog_delete_routine.xml` sobre el listado.
- Guardado real: `POST /api/routines` conserva el JSON de cada rutina y agrega `pictogram_id` solo cuando hay una selección. Ausente, null o vacío equivale a sin pictograma.
- Lectura real: `reminded` y `completed` solo si vienen en `GET /api/routines`.
- Vista previa: mismos layouts, colección en memoria descartada al cerrar la actividad.
