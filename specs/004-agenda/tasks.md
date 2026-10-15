# Tasks: Agenda, turnos y vacunas

**Input**: specs/004-agenda/plan.md

## Implementation

- [x] T001 Reglas de fecha, orden, vacunas y plan de avisos, con pruebas
- [x] T002 Pantallas de Turnos, Vacunas y Nuevo turno
- [x] T003 Persistencia local, operaciones por id y cancelación de alarmas
- [x] T004 Vista previa con los mismos layouts y selectores reales
- [x] T005 Compilar, probar y comparar capturas en el S25

La comparación en el S25 quedó hecha el 14 de octubre de 2026 para Turnos, Nuevo turno, selectores, confirmación de eliminación, Vacunas y lista vacía. Inicio, Rutinas y Contenido conservan su presentación. Quedan sin comprobar el ancho de 360 dp, el texto ampliado y la entrega real de una notificación: el intento de densidad abrió el panel del sistema y se restauró 420 dpi; el broadcast al receptor no dejó un aviso publicado.
