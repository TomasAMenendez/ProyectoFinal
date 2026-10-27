# Mejora de ánimo facial en la Raspberry Pi — plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corregir la etiqueta de ánimo en la Raspberry Pi con el Face Landmarker que ya corre y un postproceso de sus blendshapes, sin sumar una red de píxeles salvo que esa puerta falle en la cámara del peluche.

**Architecture:** El ánimo sale de los blendshapes del Face Landmarker de Tasks (`face_landmarker.task`), no de FaceMesh ni de una CNN. Una función pura aplica coactivación, un gate de habla, el delta contra el neutro de la sesión, EMA e histéresis; `CameraWorker` solo la llama y publica la etiqueta ya estable. Un clasificador NumPy, y solo después HSEmotion-B0 en LiteRT, quedan detrás de una medición en esta Pi.

**Tech Stack:** Python, OpenCV, MediaPipe Tasks Face Landmarker (`face_landmarker.task`), unittest. Sin PyTorch, sin DeepFace, sin el paquete `fer`.

## Global Constraints

- Todo corre en la Raspberry Pi 5, en local, dentro del proceso que ya tiene cámara, MediaPipe, Whisper o Vosk, y Piper.
- La emoción puede publicarse a 1–2 Hz. No hace falta 30 fps.
- No agregar un segundo detector de cara ni un runtime de más de ~150 MB de RAM extra mientras el postproceso de blendshapes no haya demostrado su techo.
- No instalar DeepFace, PyTorch, timm, ni el paquete pip `fer`.
- No hay NPU Hailo.
- No tocar Whisper, Piper, el VAD, los silencios 1.2 s / 3.0 s, ni el landmarker de manos del piedra-papel-tijera, en este plan.
- Etiquetas de cámara que el resto del sistema ya entiende: feliz, triste, sorprendido, enojado, neutral.
- Los coeficientes numéricos nuevos son punto de partida a calibrar con la cámara del peluche, no verdades medidas.
- Los datasets de adultos (FER2013, AffectNet, RAF-DB) fallan en niños, sobre todo leyendo la boca abierta como sorpresa. No entrenar ni adoptar un modelo de píxeles como primer paso.

---

## Recomendación

Una sola vía, en este orden. Cada fase se detiene si la anterior ya cumple.

1. **Fase 0.** Leer el arranque en la Pi y anotar cuatro situaciones. Cero cambios de modelo.
2. **Fase 1.** Si el ánimo entra por FaceMesh, dejar solo el Face Landmarker de Tasks. No correr los dos.
3. **Fase 2.** Sustituir la fórmula por gate de habla, coactivación, EMA, histéresis, neutro de sesión y un score de neutral que no sea `1 - score`.
4. **Fase 3.** Puerta en la Pi. Si triste, enojado y feliz se separan y hablar ya no es sorpresa, parar. No agregar una CNN.
5. **Fase 4.** Solo si el techo son los blendshapes: clasificador NumPy sobre los 52 coeficientes, entrenado con clips de esta cámara. La boca abierta se etiqueta habla o neutral, no sorpresa. Sigue sin TensorFlow.
6. **Fase 5.** Solo si la fase 4 no separa: probar HSEmotion-B0 LiteRT a 1 Hz. Si el grafo o la RAM no caben con el audio, no se adopta. OpenCV MobileFaceNet y el tflite de 91 KB son alternativas de esta misma fase, en serie.
7. **Resolución.** Bajar a ~480 px el lado largo que ve el landmarker es un ajuste de CPU aparte, después de que la etiqueta sea correcta y solo si la Pi se ahoga. No es la primera palanca.

Quedan detrás, y no se instalan en las fases 0–4: DeepFace, el paquete `fer`, EmotiEffNet B2, MobileViT, PyTorch, FERPlus ONNX, Hailo, ncnn como dependencia nueva y LITE-FER. En niños, una CNN de píxeles entrenada con adultos lee la boca abierta como sorpresa; en vídeo espontáneo de niños EmotiEffLib queda cerca de 0,30–0,42. Por eso el primer reemplazo, si la heurística no alcanza, es un lineal sobre los blendshapes de esta cámara, no una red.

## Mapa de archivos

| Archivo | Responsabilidad |
| --- | --- |
| `ProyectoParaRasperrypiV5/camera_emotion.py` | Fórmula pura, estado temporal y, en la fase de CPU, el tamaño que ve el landmarker. No abre la cámara. |
| `ProyectoParaRasperrypiV5/workers.py` | `CameraWorker` elige Tasks, llama a `AnimoFacial` y publica. No cambia `EmotionReactor` ni el landmarker de manos. |
| `ProyectoParaRasperrypiV5/test_camera_emotion.py` | Unittest de la fórmula, la histéresis y los silencios de 1,2 s / 3,0 s. |
| `ProyectoParaRasperrypiV5/camera_emotion_lineal.py` | Solo fase 4. Entrena y predice con NumPy. No lo importa `app.py` hasta que la puerta de esa fase pase. |
| `ProyectoParaRasperrypiV5/test_camera_emotion_lineal.py` | Solo fase 4. Prueba el álgebra con vectores sintéticos, que no son un dataset infantil. |
| `ProyectoParaRasperrypiV5/bench_emocion_litert.py` | Solo fase 5. Script de medición, fuera del arranque del peluche. |

Los tests de esta carpeta importan `workers` como módulo de primer nivel. El comando corre con el directorio de trabajo `ProyectoParaRasperrypiV5`:

```text
python -m unittest test_camera_emotion.py -v
```

Desde la raíz del repo ese import no resuelve.

## Pseudocódigo acordado

Los pesos son iguales en las regiones que entran. Si un término no entra (parpadeo, o mandíbula sin ojos), su peso es 0 y el promedio se reparte entre el resto. Calibrar después con la cámara del peluche.

```text
d[k] = max(0, s[k] - b[k])
blink = mean(eyeBlink L/R)
feliz = w1*mouthSmile + w2*cheekSquint + w3*eyeSquint_si_no_parpadeo
triste = mean(mouthFrown, browInnerUp, mouthShrugLower)
enojado = mean(browDown, eyeSquint_si_no_parpadeo, mouthPress)
ojos_cej = mean(eyeWide, browOuterUp)
sorprendido = ojos_cej; jawOpen solo suma si ojos_cej >= MARGEN_SORPRESA
ema + histéresis; si ganadora - segunda < MARGEN o bajo umbral → neutral
publicar solo si el candidato se repite K veces, o limpiar si no hay cara ~1 s
```

Constantes de partida, todas en `camera_emotion.py`:

| Nombre | Valor | Papel |
| --- | --- | --- |
| `PESO_FELIZ_SONRISA`, `PESO_FELIZ_MEJILLA`, `PESO_FELIZ_OJO` | 1, 1, 1 | Regiones de feliz. El ojo vale 0 si hay parpadeo. |
| `PESO_ENOJO_CEJA`, `PESO_ENOJO_OJO`, `PESO_ENOJO_BOCA` | 1, 1, 1 | `noseSneer` no entra. El ojo vale 0 si hay parpadeo. |
| `PESO_SORPRESA_OJOS`, `PESO_SORPRESA_MANDIBULA` | 1, 1 | La mandíbula vale 0 si `ojos_cej` no llega al margen. `mouthFunnel` no entra. |
| `UMBRAL_PARPADEO` | 0,50 | Por encima, `eyeSquint` no vota. |
| `MARGEN_SORPRESA` | 0,25 | Por debajo, la boca abierta no es sorpresa. |
| `UMBRAL_ETIQUETA` | 0,20 | Por debajo, neutral. |
| `MARGEN_ETIQUETA` | 0,08 | Si ganadora y segunda están más cerca, neutral. |
| `EMA_ALPHA` | 0,35 | Suavizado. A 5 fps es un punto de partida, no una constante de papel. |
| `K_REPETICIONES` | 3 | Unas 0,6 s a 5 fps. Cae en la ventana de 0,5–1 s. |
| `VENTANA_SIN_CARA_S` | 1,0 | Sin cara, se limpia la etiqueta pegada. |
| `VENTANA_NEUTRO_S` | 1,5 | Cara quieta para la mediana de sesión. |
| `UMBRAL_MOVIMIENTO` | 0,04 | Fracción del cuadro. Si el centro se mueve más, la ventana no vale. |

El score de neutral es la activación ganadora que se rechazó (sigue baja). No es `1 - esa activación`.

`EmotionReactor.NORMAL_SILENCE` sigue en 1,2 y `EXTENDED_SILENCE` en 3,0. Este plan no los edita. La forma de no alargar el silencio es no publicar `triste` ni `enojado` en un frame de habla o en un ceño de reposo. `get_silence_threshold` sigue sin mirar el score: cualquier `triste` o `enojado` publicado alarga la pausa.

Con piedra-papel-tijera activo, el bucle hace `continue` antes del ánimo. No cambiar ese `continue` ni `rps_hand.py`. El diagnóstico de las fases 0 y 3 se hace con el juego apagado.

---

### Task 1: Fase 0 — diagnóstico en la Pi

**Files:**
- Leer: `ProyectoParaRasperrypiV5/workers.py` (arranque de `CameraWorker._run` y el `continue` de manos)
- Leer en la Pi: la consola de la app y, si arrancó con debug, el último `ProyectoParaRasperrypiV5/logs/sistema_*.txt`
- No modificar código de producto.

**Produces:** Cinco líneas escritas en los huecos de esta tarea. Deciden si la fase 1 tiene que cambiar el camino y sirven de antes para la fase 3. No deciden solas que haga falta una CNN.

- [ ] **Step 1: Confirmar el camino sin editar nada**

En el arranque, una de estas dos líneas sale por el mensaje de log de la cámara:

- `MediaPipe FaceMesh habilitado` — ese camino procesa la malla y no publica emoción.
- `MediaPipe Tasks Face Landmarker habilitado` — ese camino sí llama a `_infer_emotion_from_blendshapes`.

`requirements.txt` pide `mediapipe` sin versión. `mp.solutions` desapareció en MediaPipe 0.10.31+. La línea del log manda sobre la versión supuesta.

- [ ] **Step 2: Anotar cuatro situaciones, con el juego de manos apagado**

Mirar el estado de emoción en la interfaz y, con debug, las líneas `emoción=`. Unos diez segundos cada una, sin cambiar pesos:

1. Quieto, mirando a la cámara.
2. Sonrisa.
3. Habla con la boca abierta, sin poner cara de susto.
4. Ceño de reposo (la cara quieta, sin actuar enojo) y, aparte, un ceño claro de unos dos segundos.

- [ ] **Step 3: Escribir el resultado en esta tarea**

- Arranque:
- Quieto:
- Sonrisa:
- Habla con boca abierta:
- Ceño:

- [ ] **Step 4: Criterio de parada**

La fase 0 no corrige el ánimo. Seguir a la fase 1 si el arranque dice FaceMesh, o si el código todavía prefiere FaceMesh cuando `mp.solutions.face_mesh` existe. Si el arranque ya dice Tasks, la fase 1 igual quita esa preferencia para que un `mediapipe` viejo no vuelva a tragarse la emoción; la comprobación en la Pi es solo que la línea de Tasks sigue. No saltar a la fase 4 ni a la 5 con estas notas.

---

### Task 2: Fase 1 — un solo landmarker para el ánimo

**Files:**
- Modify: `ProyectoParaRasperrypiV5/workers.py` (`CameraWorker._run`)
- Test: `ProyectoParaRasperrypiV5/test_camera_emotion.py`

**Produces:** Con `infer_emotion` en verdadero, la cámara inicializa `_create_tasks_face_landmarker` y no construye `mp.solutions.face_mesh.FaceMesh`. El log de ánimo posible es `MediaPipe Tasks Face Landmarker habilitado`. El landmarker de manos, `apply_camera_capture_size` y `frame_rate = 5` quedan como están.

**Interfaces:**
- Consumes: `_create_tasks_face_landmarker`, `_ensure_face_landmarker_model`, `_process_tasks_frame`.
- Produces: el bucle de emoción solo entra por `tasks_face_enabled`.

- [ ] **Step 1: Escribir el test que falla**

Agregar en `test_camera_emotion.py`:

```python
from pathlib import Path


class TestAnimoNoUsaFaceMesh(unittest.TestCase):
    def test_el_animo_no_habilita_facemesh(self) -> None:
        origen = (Path(__file__).resolve().parent / "workers.py").read_text(encoding="utf-8")
        self.assertNotIn("MediaPipe FaceMesh habilitado", origen)
        self.assertIn("MediaPipe Tasks Face Landmarker habilitado", origen)
        self.assertNotIn("face_mesh.FaceMesh", origen)
```

- [ ] **Step 2: Correr el test y verlo fallar**

Run desde `ProyectoParaRasperrypiV5`:

```text
python -m unittest test_camera_emotion.TestAnimoNoUsaFaceMesh -v
```

Expected: FAIL, porque hoy `_run` contiene `MediaPipe FaceMesh habilitado` y `FaceMesh(`.

- [ ] **Step 3: Quitar FaceMesh del ánimo**

En `CameraWorker._run`, el bloque que hoy hace `hasattr(mp, "solutions")` y construye `FaceMesh` queda así:

```python
if self.infer_emotion:
    tasks_landmarker = self._create_tasks_face_landmarker()
    if tasks_landmarker is not None:
        tasks_face_enabled = True
        _queue_message_with_semaphore(
            self.message_queue,
            self.message_semaphore,
            "log",
            "MediaPipe Tasks Face Landmarker habilitado",
        )
    else:
        raise RuntimeError(
            "No se pudo inicializar deteccion facial. "
            "Instala/usa una version de MediaPipe compatible o habilita descarga del modelo face_landmarker.task."
        )
```

En el bucle, borrar la rama que hace `face_mesh.process(...)`. Si `tasks_face_enabled` y el landmarker existen, se sigue llamando a `_process_tasks_frame`. Si no, se empuja el frame como ya hace el `else`. Borrar variables y el `close()` que solo servían a FaceMesh.

No crear un segundo `FaceLandmarker`. No cambiar `HandLandmarker`, el `continue` de `rps_hands_enabled`, ni las opciones de `_create_tasks_face_landmarker` (`output_face_blendshapes=True`, una cara, modo VIDEO).

- [ ] **Step 4: Correr el test**

```text
python -m unittest test_camera_emotion.py -v
```

Expected: PASS, incluido `test_emocion_encendida_a_5_fps`.

- [ ] **Step 5: Qué mirar en la Pi**

Arrancar el peluche. El log de cámara dice `MediaPipe Tasks Face Landmarker habilitado` y no dice `MediaPipe FaceMesh habilitado`. Aparece emoción en el estado (aunque la fórmula vieja siga equivocándose). El audio no se entrecorta: no hay un overflow de entrada nuevo respecto del arranque anterior. No hace falta desplegar para escribir este plan; el despliegue es de quien ejecute la tarea.

- [ ] **Step 6: Criterio de parada**

Si Tasks no carga (`face_landmarker.task` ausente y sin red para la descarga que ya hace `_ensure_face_landmarker_model`), parar aquí y resolver el archivo del modelo. No inventar otro detector. Si Tasks carga, seguir a la fase 2 aunque la etiqueta vieja siga mal: la fase 1 no promete el ánimo, solo el canal que sí publica.

- [ ] **Step 7: Commit**

```bash
git add ProyectoParaRasperrypiV5/workers.py ProyectoParaRasperrypiV5/test_camera_emotion.py
git commit -m "El ánimo facial usa solo el Face Landmarker de Tasks."
```

---

### Task 3: Fase 2 — fórmula, gate, suavizado y neutro

**Files:**
- Create: `ProyectoParaRasperrypiV5/camera_emotion.py`
- Modify: `ProyectoParaRasperrypiV5/workers.py` (`CameraWorker.__init__`, `_process_tasks_frame`, el tramo que publica `emotion`)
- Test: `ProyectoParaRasperrypiV5/test_camera_emotion.py`

**Produces:** `puntajes_animo` y `elegir_etiqueta` deciden la etiqueta de un frame. `AnimoFacial.observar` aplica EMA, exige `K_REPETICIONES` y limpia la etiqueta si no hay cara durante `VENTANA_SIN_CARA_S`. `CameraWorker` publica solo lo que `observar` devuelve. Los silencios de `EmotionReactor` no cambian.

**Interfaces:**
- Consumes: blendshapes con `category_name` y `score` (o un `dict` con esas claves) y landmarks con `.x` y `.y`.
- Produces:
  - `coeficientes_desde_mediapipe(blendshapes) -> dict[str, float]`
  - `puntajes_animo(crudos, neutro=None) -> dict[str, float]` con claves `feliz`, `triste`, `sorprendido`, `enojado`
  - `elegir_etiqueta(puntajes) -> tuple[str, float]`
  - `centro_de_landmarks(landmarks) -> tuple[float, float] | None`
  - `AnimoFacial(calibrar_neutro: bool = True).observar(crudos, ahora_s, centro=None) -> dict | None`
  - Retorno de `observar`: `None` si no toca publicar; `{"label": "feliz"|"triste"|"sorprendido"|"enojado"|"neutral", "score": float}` al confirmar; `{"label": None, "score": 0.0}` para limpiar el contexto.
  - `ORDEN_BLENDSHAPES: tuple[str, ...]` de 52 nombres, para la fase 4.

- [ ] **Step 1: Escribir los tests que fallan**

Conservar `TestCameraEmotionDefaults`. Agregar los siguientes. Los asserts miran la etiqueta, no un decimal fino. `SONRISA` y `ENOJO` van al inicio del módulo de test:

```python
import unittest

from camera_emotion import (
    VENTANA_SIN_CARA_S,
    AnimoFacial,
    elegir_etiqueta,
    puntajes_animo,
)
from workers import CameraWorker, EmotionReactor

SONRISA = {
    "mouthSmileLeft": 0.9,
    "mouthSmileRight": 0.9,
    "cheekSquintLeft": 0.9,
    "cheekSquintRight": 0.9,
}
ENOJO = {
    "browDownLeft": 0.9,
    "browDownRight": 0.9,
    "mouthPressLeft": 0.9,
    "mouthPressRight": 0.9,
}
HABLA = {
    "jawOpen": 0.9,
    "mouthFunnel": 0.9,
    "eyeWideLeft": 0.05,
    "eyeWideRight": 0.05,
    "browOuterUpLeft": 0.05,
    "browOuterUpRight": 0.05,
}
SORPRESA = {
    "eyeWideLeft": 0.85,
    "eyeWideRight": 0.85,
    "browOuterUpLeft": 0.85,
    "browOuterUpRight": 0.85,
    "jawOpen": 0.4,
}


def _etiquetas(frames, *, dt=0.2, calibrar_neutro=False):
    animo = AnimoFacial(calibrar_neutro=calibrar_neutro)
    salidas = []
    t = 0.0
    for frame in frames:
        decision = animo.observar(frame, t, (0.5, 0.5))
        if decision is not None and decision.get("label"):
            salidas.append(decision["label"])
        t += dt
    return animo, salidas


class TestFormulaDeAnimo(unittest.TestCase):
    def test_boca_abierta_sin_ojos_anchos_no_es_sorpresa(self) -> None:
        etiqueta, _score = elegir_etiqueta(puntajes_animo(HABLA))
        self.assertNotEqual(etiqueta, "sorprendido")
        self.assertEqual(etiqueta, "neutral")

    def test_ojos_y_cejas_altos_si_es_sorpresa(self) -> None:
        etiqueta, _score = elegir_etiqueta(puntajes_animo(SORPRESA))
        self.assertEqual(etiqueta, "sorprendido")

    def test_sonrisa_y_mejilla_es_feliz(self) -> None:
        etiqueta, _score = elegir_etiqueta(puntajes_animo(SONRISA))
        self.assertEqual(etiqueta, "feliz")

    def test_frunce_ceja_interna_y_encogimiento_es_triste(self) -> None:
        triste = {
            "mouthFrownLeft": 0.8,
            "mouthFrownRight": 0.8,
            "browInnerUp": 0.8,
            "mouthShrugLower": 0.8,
        }
        etiqueta, _score = elegir_etiqueta(puntajes_animo(triste))
        self.assertEqual(etiqueta, "triste")

    def test_nariz_fruncida_sola_no_es_enojo(self) -> None:
        nariz = {"noseSneerLeft": 0.9, "noseSneerRight": 0.9}
        etiqueta, _score = elegir_etiqueta(puntajes_animo(nariz))
        self.assertNotEqual(etiqueta, "enojado")
        self.assertEqual(etiqueta, "neutral")

    def test_ceja_de_reposo_resta_el_neutro_y_no_es_enojo(self) -> None:
        reposo = {
            "browDownLeft": 0.55,
            "browDownRight": 0.55,
            "mouthPressLeft": 0.4,
            "mouthPressRight": 0.4,
        }
        etiqueta, _score = elegir_etiqueta(puntajes_animo(reposo, neutro=reposo))
        self.assertEqual(etiqueta, "neutral")

    def test_ceja_por_encima_del_neutro_es_enojo(self) -> None:
        reposo = {
            "browDownLeft": 0.55,
            "browDownRight": 0.55,
            "mouthPressLeft": 0.4,
            "mouthPressRight": 0.4,
        }
        mas = {
            "browDownLeft": 0.95,
            "browDownRight": 0.95,
            "mouthPressLeft": 0.9,
            "mouthPressRight": 0.9,
        }
        etiqueta, _score = elegir_etiqueta(puntajes_animo(mas, neutro=reposo))
        self.assertEqual(etiqueta, "enojado")

    def test_neutral_debil_no_usa_el_complemento_del_score(self) -> None:
        debil = {"mouthSmileLeft": 0.05, "mouthSmileRight": 0.05}
        puntajes = puntajes_animo(debil)
        etiqueta, score = elegir_etiqueta(puntajes)
        ganadora = max(puntajes.values())
        self.assertEqual(etiqueta, "neutral")
        self.assertLess(score, 0.5)
        self.assertNotAlmostEqual(score, 1.0 - ganadora, places=2)


class TestPublicacionDeAnimo(unittest.TestCase):
    def test_un_frame_de_ceja_entre_sonrisas_no_cambia_lo_publicado(self) -> None:
        frames = [SONRISA, SONRISA, SONRISA, ENOJO, SONRISA, SONRISA, SONRISA]
        _animo, salidas = _etiquetas(frames)
        self.assertEqual(salidas, ["feliz"])

    def test_enojo_sostenido_si_reemplaza_la_sonrisa(self) -> None:
        frames = [SONRISA] * 3 + [ENOJO] * 8
        _animo, salidas = _etiquetas(frames)
        self.assertEqual(salidas[0], "feliz")
        self.assertEqual(salidas[-1], "enojado")

    def test_sin_cara_durante_la_ventana_limpia_lo_publicado(self) -> None:
        animo = AnimoFacial(calibrar_neutro=False)
        t = 0.0
        for _ in range(3):
            animo.observar(SONRISA, t, (0.5, 0.5))
            t += 0.2
        self.assertEqual(animo.publicada, "feliz")
        self.assertIsNone(animo.observar(None, t, None))
        limpieza = animo.observar(None, t + VENTANA_SIN_CARA_S, None)
        self.assertEqual(limpieza, {"label": None, "score": 0.0})
        self.assertIsNone(animo.publicada)
        self.assertIsNone(animo.observar(SONRISA, t + VENTANA_SIN_CARA_S + 0.2, (0.5, 0.5)))

    def test_cara_que_se_mueve_no_fija_el_neutro(self) -> None:
        animo = AnimoFacial(calibrar_neutro=True)
        centros = ((0.10, 0.50), (0.50, 0.50), (0.90, 0.50), (0.20, 0.50))
        t = 0.0
        for centro in centros:
            animo.observar(SONRISA, t, centro)
            t += 0.6
        self.assertFalse(animo.neutro_listo)

    def test_cara_quieta_fija_la_mediana_y_la_conserva_sin_cara(self) -> None:
        animo = AnimoFacial(calibrar_neutro=True)
        t = 0.0
        while t <= 1.5:
            animo.observar({}, t, (0.50, 0.50))
            t += 0.5
        self.assertTrue(animo.neutro_listo)
        animo.observar(None, t, None)
        animo.observar(None, t + VENTANA_SIN_CARA_S, None)
        self.assertTrue(animo.neutro_listo)
        self.assertIsNone(animo.publicada)


class TestSilenciosSinCambio(unittest.TestCase):
    def test_silencios_de_audio_siguen_en_1_2_y_3_0(self) -> None:
        self.assertEqual(EmotionReactor.NORMAL_SILENCE, 1.2)
        self.assertEqual(EmotionReactor.EXTENDED_SILENCE, 3.0)
```

`test_emocion_encendida_a_5_fps` sigue exigiendo `frame_rate == 5` e `infer_emotion is True`. No bajar esos defaults en esta fase.

- [ ] **Step 2: Correr los tests y verlos fallar**

```text
python -m unittest test_camera_emotion.py -v
```

Expected: FAIL con `ModuleNotFoundError` o `ImportError` de `camera_emotion`. `test_emocion_encendida_a_5_fps` puede pasar si el import de `camera_emotion` se hace dentro de los tests nuevos y el import de `workers` sigue arriba. Si el archivo importa `camera_emotion` al inicio y el módulo no existe, falla toda la corrida: es el fallo esperado.

- [ ] **Step 3: Implementar `camera_emotion.py`**

Crear el módulo con este contrato. Los comentarios de calibración se quedan junto a las constantes. No copiar coeficientes de papers de iPhone.

```python
"""Postproceso de ánimo sobre los blendshapes del Face Landmarker.

Los pesos y umbrales son punto de partida para la cámara del peluche.
"""
from __future__ import annotations

from statistics import median
from typing import Mapping

ORDEN_BLENDSHAPES: tuple[str, ...] = (
    "_neutral",
    "browDownLeft",
    "browDownRight",
    "browInnerUp",
    "browOuterUpLeft",
    "browOuterUpRight",
    "cheekPuff",
    "cheekSquintLeft",
    "cheekSquintRight",
    "eyeBlinkLeft",
    "eyeBlinkRight",
    "eyeLookDownLeft",
    "eyeLookDownRight",
    "eyeLookInLeft",
    "eyeLookInRight",
    "eyeLookOutLeft",
    "eyeLookOutRight",
    "eyeLookUpLeft",
    "eyeLookUpRight",
    "eyeSquintLeft",
    "eyeSquintRight",
    "eyeWideLeft",
    "eyeWideRight",
    "jawForward",
    "jawLeft",
    "jawOpen",
    "jawRight",
    "mouthClose",
    "mouthDimpleLeft",
    "mouthDimpleRight",
    "mouthFrownLeft",
    "mouthFrownRight",
    "mouthFunnel",
    "mouthLeft",
    "mouthLowerDownLeft",
    "mouthLowerDownRight",
    "mouthPressLeft",
    "mouthPressRight",
    "mouthPucker",
    "mouthRight",
    "mouthRollLower",
    "mouthRollUpper",
    "mouthShrugLower",
    "mouthShrugUpper",
    "mouthSmileLeft",
    "mouthSmileRight",
    "mouthStretchLeft",
    "mouthStretchRight",
    "mouthUpperUpLeft",
    "mouthUpperUpRight",
    "noseSneerLeft",
    "noseSneerRight",
)

# Calibrar en la cámara del peluche. Pesos iguales en las regiones que entran.
PESO_FELIZ_SONRISA = 1.0
PESO_FELIZ_MEJILLA = 1.0
PESO_FELIZ_OJO = 1.0
PESO_ENOJO_CEJA = 1.0
PESO_ENOJO_OJO = 1.0
PESO_ENOJO_BOCA = 1.0
PESO_SORPRESA_OJOS = 1.0
PESO_SORPRESA_MANDIBULA = 1.0

UMBRAL_PARPADEO = 0.50
MARGEN_SORPRESA = 0.25
UMBRAL_ETIQUETA = 0.20
MARGEN_ETIQUETA = 0.08
EMA_ALPHA = 0.35
K_REPETICIONES = 3
VENTANA_SIN_CARA_S = 1.0
VENTANA_NEUTRO_S = 1.5
UMBRAL_MOVIMIENTO = 0.04

_CLAVES = ("feliz", "triste", "sorprendido", "enojado")


def _promedio_ponderado(pares: list[tuple[float, float]]) -> float:
    peso = sum(p for _valor, p in pares if p > 0.0)
    if peso <= 0.0:
        return 0.0
    return sum(valor * p for valor, p in pares if p > 0.0) / peso


def _par(valores: Mapping[str, float], izquierda: str, derecha: str) -> float:
    return (float(valores.get(izquierda, 0.0)) + float(valores.get(derecha, 0.0))) / 2.0


def coeficientes_desde_mediapipe(blendshapes) -> dict[str, float]:
    crudos = {nombre: 0.0 for nombre in ORDEN_BLENDSHAPES}
    for item in blendshapes or []:
        if isinstance(item, Mapping):
            nombre = str(item.get("category_name") or "")
            valor = item.get("score", 0.0)
        else:
            nombre = str(getattr(item, "category_name", "") or "")
            valor = getattr(item, "score", 0.0)
        if nombre:
            crudos[nombre] = float(valor)
    return crudos


def delta_sobre_neutro(
    crudos: Mapping[str, float],
    neutro: Mapping[str, float] | None,
) -> dict[str, float]:
    if not neutro:
        return {clave: float(valor) for clave, valor in crudos.items()}
    claves = set(crudos) | set(neutro)
    return {
        clave: max(0.0, float(crudos.get(clave, 0.0)) - float(neutro.get(clave, 0.0)))
        for clave in claves
    }


def puntajes_animo(
    crudos: Mapping[str, float],
    neutro: Mapping[str, float] | None = None,
) -> dict[str, float]:
    d = delta_sobre_neutro(crudos, neutro)
    parpadeo = _par(d, "eyeBlinkLeft", "eyeBlinkRight")
    ojo_entra = parpadeo < UMBRAL_PARPADEO
    ojo = _par(d, "eyeSquintLeft", "eyeSquintRight")

    feliz = _promedio_ponderado([
        (_par(d, "mouthSmileLeft", "mouthSmileRight"), PESO_FELIZ_SONRISA),
        (_par(d, "cheekSquintLeft", "cheekSquintRight"), PESO_FELIZ_MEJILLA),
        (ojo, PESO_FELIZ_OJO if ojo_entra else 0.0),
    ])
    triste = _promedio_ponderado([
        (_par(d, "mouthFrownLeft", "mouthFrownRight"), 1.0),
        (float(d.get("browInnerUp", 0.0)), 1.0),
        (float(d.get("mouthShrugLower", 0.0)), 1.0),
    ])
    enojado = _promedio_ponderado([
        (_par(d, "browDownLeft", "browDownRight"), PESO_ENOJO_CEJA),
        (ojo, PESO_ENOJO_OJO if ojo_entra else 0.0),
        (_par(d, "mouthPressLeft", "mouthPressRight"), PESO_ENOJO_BOCA),
    ])
    ojos_cej = _promedio_ponderado([
        (_par(d, "eyeWideLeft", "eyeWideRight"), 1.0),
        (_par(d, "browOuterUpLeft", "browOuterUpRight"), 1.0),
    ])
    mandibula_entra = ojos_cej >= MARGEN_SORPRESA
    sorprendido = _promedio_ponderado([
        (ojos_cej, PESO_SORPRESA_OJOS),
        (float(d.get("jawOpen", 0.0)), PESO_SORPRESA_MANDIBULA if mandibula_entra else 0.0),
    ])
    return {
        "feliz": feliz,
        "triste": triste,
        "sorprendido": sorprendido,
        "enojado": enojado,
    }


def elegir_etiqueta(puntajes: Mapping[str, float]) -> tuple[str, float]:
    ordenadas = sorted(puntajes.items(), key=lambda par: par[1], reverse=True)
    if not ordenadas:
        return "neutral", 0.0
    ganadora, alto = ordenadas[0]
    segunda = ordenadas[1][1] if len(ordenadas) > 1 else 0.0
    if alto < UMBRAL_ETIQUETA or (alto - segunda) < MARGEN_ETIQUETA:
        return "neutral", float(alto)
    return ganadora, float(alto)


def centro_de_landmarks(landmarks) -> tuple[float, float] | None:
    xs: list[float] = []
    ys: list[float] = []
    for punto in landmarks or []:
        xs.append(float(punto.x))
        ys.append(float(punto.y))
    if not xs:
        return None
    return (sum(xs) / len(xs), sum(ys) / len(ys))


class AnimoFacial:
    """EMA, histéresis y mediana de una sesión. No abre la cámara."""

    def __init__(self, calibrar_neutro: bool = True) -> None:
        self.calibrar_neutro = calibrar_neutro
        self._ema = {clave: 0.0 for clave in _CLAVES}
        self._racha_etiqueta = ""
        self._racha = 0
        self.publicada: str | None = None
        self.neutro: dict[str, float] | None = None
        self.neutro_listo = False
        self._muestras: list[dict[str, float]] = []
        self._t0_neutro: float | None = None
        self._centro0: tuple[float, float] | None = None
        self._sin_cara_desde: float | None = None
        self._limpieza_enviada = False

    def observar(
        self,
        crudos: Mapping[str, float] | None,
        ahora_s: float,
        centro: tuple[float, float] | None = None,
    ) -> dict | None:
        if crudos is None:
            return self._sin_cara(ahora_s)
        self._sin_cara_desde = None
        self._limpieza_enviada = False
        self._actualizar_neutro(crudos, ahora_s, centro)
        neutro = self.neutro if self.neutro_listo else None
        instantaneos = puntajes_animo(crudos, neutro)
        for clave in _CLAVES:
            previo = self._ema[clave]
            self._ema[clave] = EMA_ALPHA * instantaneos[clave] + (1.0 - EMA_ALPHA) * previo
        etiqueta, score = elegir_etiqueta(self._ema)
        if etiqueta == self._racha_etiqueta:
            self._racha += 1
        else:
            self._racha_etiqueta = etiqueta
            self._racha = 1
        if self._racha < K_REPETICIONES or etiqueta == self.publicada:
            return None
        self.publicada = etiqueta
        return {"label": etiqueta, "score": float(score)}

    def _sin_cara(self, ahora_s: float) -> dict | None:
        if self._sin_cara_desde is None:
            self._sin_cara_desde = ahora_s
            return None
        if (ahora_s - self._sin_cara_desde) < VENTANA_SIN_CARA_S or self._limpieza_enviada:
            return None
        self._limpieza_enviada = True
        self._ema = {clave: 0.0 for clave in _CLAVES}
        self._racha_etiqueta = ""
        self._racha = 0
        self.publicada = None
        return {"label": None, "score": 0.0}

    def _actualizar_neutro(
        self,
        crudos: Mapping[str, float],
        ahora_s: float,
        centro: tuple[float, float] | None,
    ) -> None:
        if self.neutro_listo or not self.calibrar_neutro:
            return
        if centro is None:
            self._reiniciar_ventana()
            return
        if self._centro0 is None or self._t0_neutro is None:
            self._centro0 = centro
            self._t0_neutro = ahora_s
            self._muestras = [dict(crudos)]
            return
        dx = abs(centro[0] - self._centro0[0])
        dy = abs(centro[1] - self._centro0[1])
        if dx > UMBRAL_MOVIMIENTO or dy > UMBRAL_MOVIMIENTO:
            self._centro0 = centro
            self._t0_neutro = ahora_s
            self._muestras = [dict(crudos)]
            return
        self._muestras.append(dict(crudos))
        if (ahora_s - self._t0_neutro) < VENTANA_NEUTRO_S:
            return
        claves = set().union(*(muestra.keys() for muestra in self._muestras))
        self.neutro = {
            clave: float(median(muestra.get(clave, 0.0) for muestra in self._muestras))
            for clave in claves
        }
        self.neutro_listo = True

    def _reiniciar_ventana(self) -> None:
        self._muestras = []
        self._t0_neutro = None
        self._centro0 = None
```

`noseSneer` y `mouthFunnel` no se leen. La primera ventana quieta de 1,5 s fija la mediana una vez; un centro que salta más de `UMBRAL_MOVIMIENTO` la descarta. La mediana sobrevive a la limpieza por falta de cara. Quien ejecute la fase 3 tiene que mirar el arranque con la cara quieta: si el niño sonríe durante esos 1,5 s, la sonrisa queda comida por la mediana hasta el próximo arranque.

- [ ] **Step 4: Conectar `CameraWorker` sin tocar el reactor ni las manos**

En `CameraWorker.__init__`, donde se asigna `self.infer_emotion = True`, crear `self._animo = AnimoFacial()`.

Sustituir el uso de `_infer_emotion_from_blendshapes` dentro de `_process_tasks_frame` por una llamada que no publique por su cuenta. El método puede quedar como envoltorio de un frame, usado solo desde tests viejos si alguien lo llama, pero el bucle publica la decisión temporal:

```python
def _decision_de_animo(self, blendshapes, landmarks, ahora_s: float):
    if not landmarks:
        return self._animo.observar(None, ahora_s, None)
    crudos = coeficientes_desde_mediapipe(blendshapes or [])
    return self._animo.observar(crudos, ahora_s, centro_de_landmarks(landmarks))
```

En el tramo de `_run` que hoy publica cuando `emotion_payload is not None`:

- Si la decisión es `None`, no encolar `emotion` ni `status`. La última etiqueta sigue hasta que la histéresis cambie o hasta la limpieza.
- Si `label` es una cadena, encolar `emotion` con `label` y `score`, y el `status` con `emotion` como ya está (`f"{label} ({score:.2f})"`). `log_action(..., f"emoción={label} ({score:.2f})")` solo cuando la etiqueta cambia.
- Si `label is None`, encolar una vez `emotion` con `{"label": None, "score": 0.0}` y `status` con `emotion` en `sin cara`. `app.py` ya hace `set_current_emotion(None)` cuando el label llega vacío. Log: `emoción=sin cara`.

No editar `EmotionReactor`, `NORMAL_SILENCE`, `EXTENDED_SILENCE`, Whisper, Piper, el VAD ni el landmarker de manos. Borrar el retorno `{"label": "neutral", "score": 1.0 - score}` de `_infer_emotion_from_blendshapes`. Si el método sigue existiendo, que delegue en `elegir_etiqueta(puntajes_animo(...))` para un frame suelto, sin EMA: la publicación real pasa por `AnimoFacial`.

El dibujo de la caja y el texto del frame pueden mostrar la última etiqueta publicada. No hace falta un segundo detector para la caja: los landmarks de Tasks ya están.

- [ ] **Step 5: Correr los tests**

```text
python -m unittest test_camera_emotion.py -v
```

Expected: PASS. Si `test_enojo_sostenido_si_reemplaza_la_sonrisa` no llega a `enojado` en 8 frames, el término de `eyeSquint` en cero está diluyendo el enojo o la histéresis no suelta la sonrisa. Un ceño con la boca apretada, sin parpadeo, tiene que poder ganar después de sostenerse. No aflojar el assert a un score decimal.

- [ ] **Step 6: Qué mirar en la Pi**

Con el juego de manos apagado, repetir las cuatro situaciones de la fase 0. En el log o en el estado: sonrisa → `emoción=feliz`; habla con boca abierta → la etiqueta no se queda en `sorprendido`; ceño de reposo → no queda `triste` ni `enojado` sostenido; un ceño claro de un par de segundos sí puede ser `enojado`. Al salir de cuadro cerca de un segundo, el estado pasa a sin cara y no conserva el enojo anterior. El audio no muestra un overflow de entrada nuevo. Esta tarea no pide desplegar para dar el plan por escrito.

- [ ] **Step 7: Criterio de parada**

No abrir la fase 4 en esta tarea. La puerta es la fase 3, en la Pi, con esta fórmula ya corriendo.

- [ ] **Step 8: Commit**

```bash
git add ProyectoParaRasperrypiV5/camera_emotion.py ProyectoParaRasperrypiV5/workers.py ProyectoParaRasperrypiV5/test_camera_emotion.py
git commit -m "El ánimo usa coactivación, histéresis y el neutro de la sesión."
```

---

### Task 4: Fase 3 — puerta de decisión en la Pi

**Files:**
- Ningún archivo de producto.
- Leer el mismo log que en la fase 0, ya con la fase 2 en la Pi.

**Produces:** Una frase: «parar» o «el techo son los blendshapes». Solo la segunda abre la fase 4.

- [ ] **Step 1: Repetir las cuatro situaciones**

Juego de manos apagado. Anotar junto a las notas de la fase 0:

- Quieto, después de ~1,5 s de cara quieta: neutral, o una etiqueta que no salta frame a frame.
- Sonrisa: `feliz`, y un ceño de un solo instante no la cambia.
- Habla con boca abierta, una frase entera: no es `sorprendido`.
- Ceño de reposo: no publica `triste` ni `enojado` de forma sostenida. Un ceño actuado sí se separa de la sonrisa.
- Al perder la cara cerca de un segundo, no queda pegada la etiqueta anterior.
- El arranque sigue diciendo Tasks, no FaceMesh.
- Durante un minuto de escucha, el audio no se entrecorta más que antes de la fase 2.

- [ ] **Step 2: Criterio de parada**

Si esas lecturas se cumplen, parar. No crear `camera_emotion_lineal.py`, no bajar la resolución y no descargar HSEmotion. El trabajo de ánimo de este plan terminó.

Seguir a la fase 4 solo si, con la fase 2 ya en la Pi, la boca abierta al hablar sigue siendo sorpresa, o triste / enojado / feliz no se separan en esas cuatro situaciones. Anotar cuál de las cuatro falló. No seguir por curiosidad de probar una CNN.

---

### Task 5: Fase 4 — lineal NumPy, solo si la fase 3 no paró

**Files:**
- Create: `ProyectoParaRasperrypiV5/camera_emotion_lineal.py`
- Create: `ProyectoParaRasperrypiV5/test_camera_emotion_lineal.py`
- Modify, solo después de que los clips de la Pi ganen a la heurística: `ProyectoParaRasperrypiV5/workers.py` (un cargador opcional dentro de `CameraWorker`, sin segundo detector)

**Produces:** Un entrenador de mínimos cuadrados, uno contra el resto, sobre el vector de 52 blendshapes en `ORDEN_BLENDSHAPES`. Tests con dos nubes sintéticas separables. Los clips reales no entran al repositorio. `CameraWorker` no carga pesos hasta que la comparación en la Pi gane.

**Interfaces:**
- Consumes: `ORDEN_BLENDSHAPES` de `camera_emotion.py`.
- Produces:
  - `vector_blendshapes(crudos: Mapping[str, float]) -> numpy.ndarray` de forma `(52,)`
  - `entrenar_lineal(vectores, etiquetas: list[str]) -> dict` con `clases`, `pesos` (lista de listas) y `sesgo`
  - `predecir_lineal(modelo, crudos) -> tuple[str, float]`
  - Las clases permitidas son las cinco del sistema. Un clip de boca abierta se etiqueta `neutral`, no `sorprendido`.

- [ ] **Step 1: Test sintético, no un dataset de niños**

El test construye en memoria dos grupos. No lee FER2013, AffectNet, RAF-DB ni clips. No escribe un json de producción.

```python
class TestLinealSintetico(unittest.TestCase):
    def test_separa_sonrisa_de_ceros(self) -> None:
        from camera_emotion import ORDEN_BLENDSHAPES
        from camera_emotion_lineal import entrenar_lineal, predecir_lineal

        i = ORDEN_BLENDSHAPES.index("mouthSmileLeft")
        j = ORDEN_BLENDSHAPES.index("mouthSmileRight")
        cero = {nombre: 0.0 for nombre in ORDEN_BLENDSHAPES}
        sonrisa = dict(cero)
        sonrisa["mouthSmileLeft"] = 0.9
        sonrisa["mouthSmileRight"] = 0.9
        muestras = [cero, cero, sonrisa, sonrisa]
        clases = ["neutral", "neutral", "feliz", "feliz"]
        # Dejar uno afuera, en los dos sentidos.
        for fuera in range(4):
            modelo = entrenar_lineal(
                [muestras[k] for k in range(4) if k != fuera],
                [clases[k] for k in range(4) if k != fuera],
            )
            etiqueta, _score = predecir_lineal(modelo, muestras[fuera])
            self.assertEqual(etiqueta, clases[fuera])
```

Esos vectores solo demuestran que el álgebra separa dos nubes. No se copian a la Pi como modelo del niño.

- [ ] **Step 2: Verlo fallar**

```text
python -m unittest test_camera_emotion_lineal.py -v
```

Expected: FAIL, módulo ausente.

- [ ] **Step 3: Implementar el lineal con NumPy, sin TensorFlow**

Dependencia nueva: ninguna. NumPy ya está en el proceso de cámara. El score es la activación ganadora. Si la ganadora no es `neutral` y no le saca `MARGEN_ETIQUETA` a la segunda, el resultado es `neutral` con esa misma activación, nunca `1 - activación`.

```python
"""Clasificador lineal sobre los 52 blendshapes. No descarga datasets."""
from __future__ import annotations

from typing import Mapping

import numpy as np

from camera_emotion import MARGEN_ETIQUETA, ORDEN_BLENDSHAPES


def vector_blendshapes(crudos: Mapping[str, float]) -> np.ndarray:
    return np.array(
        [float(crudos.get(nombre, 0.0)) for nombre in ORDEN_BLENDSHAPES],
        dtype=np.float64,
    )


def entrenar_lineal(muestras: list[Mapping[str, float]], etiquetas: list[str]) -> dict:
    clases: list[str] = []
    for etiqueta in etiquetas:
        if etiqueta not in clases:
            clases.append(etiqueta)
    rasgos = np.vstack([vector_blendshapes(muestra) for muestra in muestras])
    diseno = np.concatenate([rasgos, np.ones((len(muestras), 1))], axis=1)
    pesos: list[list[float]] = []
    sesgo: list[float] = []
    for clase in clases:
        objetivo = np.array(
            [1.0 if etiqueta == clase else 0.0 for etiqueta in etiquetas],
            dtype=np.float64,
        )
        coef, *_resto = np.linalg.lstsq(diseno, objetivo, rcond=None)
        pesos.append(coef[:-1].tolist())
        sesgo.append(float(coef[-1]))
    return {"clases": clases, "pesos": pesos, "sesgo": sesgo}


def predecir_lineal(modelo: dict, crudos: Mapping[str, float]) -> tuple[str, float]:
    rasgos = vector_blendshapes(crudos)
    activaciones = [
        float(np.dot(pesos, rasgos) + sesgo)
        for pesos, sesgo in zip(modelo["pesos"], modelo["sesgo"])
    ]
    ordenadas = sorted(
        zip(modelo["clases"], activaciones),
        key=lambda par: par[1],
        reverse=True,
    )
    ganadora, alto = ordenadas[0]
    segunda = ordenadas[1][1] if len(ordenadas) > 1 else 0.0
    if ganadora != "neutral" and (alto - segunda) < MARGEN_ETIQUETA:
        return "neutral", float(alto)
    return ganadora, float(alto)
```

- [ ] **Step 4: Correr el test sintético**

```text
python -m unittest test_camera_emotion_lineal.py test_camera_emotion.py -v
```

Expected: PASS. El de cámara no se rompe.

- [ ] **Step 5: Clips de esta cámara, en la Pi, fuera de git**

Solo si la fase 3 dijo que los blendshapes no separan. Grabar con el Face Landmarker ya en marcha, 20 clips de unos 2 s por cada una de estas etiquetas hechas a mano: `feliz`, `triste`, `enojado`, `neutral`, y 20 de habla con boca abierta etiquetados `neutral`. La misma cámara del peluche. Una persona delante, la cara completa.

No descargar FER2013, AffectNet, RAF-DB ni CAFE. No subir los clips al repositorio. El rasgo de cada clip es la mediana de los 52 coeficientes. Comparar, clip contra clip dejado afuera, la etiqueta del lineal con la de `puntajes_animo` / `elegir_etiqueta`.

Adoptar el json solo si las dos cosas se cumplen: en feliz, triste y enojado el lineal acierta más clips que la heurística, y ningún clip de boca abierta sale `sorprendido`. Si no, borrar el json, no tocar `CameraWorker`, y pasar a la fase 5.

- [ ] **Step 6: Cablear solo el json que ganó**

Si el json ganó, `CameraWorker` lo carga desde una ruta local (`models/animo_lineal.json` junto a `face_landmarker.task`). El vector entra después del mismo delta de sesión. La histéresis de `AnimoFacial` se conserva: el lineal reemplaza `puntajes_animo`, no el gate de publicación ni la limpieza sin cara. Si el archivo no está, sigue la heurística de la fase 2. No commitear clips. El json entra al repo solo con esa comparación ya anotada en esta tarea.

- [ ] **Step 7: Qué mirar en la Pi**

Las mismas cuatro situaciones. `emoción=` no marca sorpresa mientras se habla. El audio sigue igual, porque este paso no suma un runtime: es aritmética sobre 52 floats.

- [ ] **Step 8: Criterio de parada**

Si la comparación de clips no gana, no cablear el lineal y seguir a la fase 5. Si gana y las cuatro situaciones se cumplen, parar. No descargar HSEmotion.

- [ ] **Step 9: Commit**

Incluir el json solo si la puerta del step 5 pasó.

```bash
git add ProyectoParaRasperrypiV5/camera_emotion_lineal.py ProyectoParaRasperrypiV5/test_camera_emotion_lineal.py
git commit -m "Agrega un clasificador lineal de ánimo sobre los blendshapes, sin cablear pesos de juguete."
```

---

### Task 6: Fase 5 — HSEmotion-B0 LiteRT, solo si la fase 4 no separó

**Files:**
- Create: `ProyectoParaRasperrypiV5/bench_emocion_litert.py` (script de medición; `app.py` no lo importa)
- Modify, solo si la medición pasa: `ProyectoParaRasperrypiV5/workers.py` y `ProyectoParaRasperrypiV5/requirements.txt`

**Produces:** Una medición en la Pi de TEO de `hsemotion_b0_fp16.tflite` (~8 MB, ficha Hugging Face `litert-community/HSEmotion-B0-LiteRT`). Si no cabe, se prueba en serie el ONNX MobileFaceNet de OpenCV Zoo (4,79 MB) y, al final, el tflite Mini-Xception de 91 KB como archivo suelto. Uno solo puede quedar cableado, y solo si la medición pasa.

**Interfaces:**
- Consumes: la caja de los landmarks que Tasks ya calculó. No hay otro detector de cara.
- Produces: a 1 Hz, como máximo, una de las cinco etiquetas. `Contempt`, `Disgust` y `Fear` no se publican (no se mapean a sorpresa ni a enojo). `Happiness→feliz`, `Sadness→triste`, `Surprise→sorprendido`, `Anger→enojado`, `Neutral→neutral`. El mismo mapa de cinco clases vale para el ONNX y para el tflite de 91 KB.

- [ ] **Step 1: Medir antes de cablear**

El script carga el tflite con el intérprete LiteRT que haya en la Pi (`ai-edge-litert` o `tflite-runtime`). No agregar PyTorch, timm, DeepFace ni el paquete `fer`. Hilos del intérprete: 2. Recorta con la caja de landmarks ya calculada, margen 0,20, y reescala al tamaño del tensor de entrada del propio archivo (en un EfficientNet-B0 habitual, 224×224; leer el tensor y usar ese número). Veinte inferencias, imprimir la mediana en milisegundos y el RSS del proceso antes y después.

Correr el script un minuto con el audio del peluche en marcha y el minuto equivalente solo con blendshapes.

- [ ] **Step 2: Criterio para no adoptar este archivo**

No cablearlo si ocurre cualquiera de estas tres:

- El intérprete o XNNPACK rechaza el grafo FP16.
- El RSS sube más de 150 MB.
- En ese minuto aparece overflow de audio (`input overflow` o el aviso de callback de audio) que no estaba en el minuto con solo blendshapes.

Si el archivo se rechaza, no reintentar con más hilos ni con otro delegate a ciegas. Pasar al step 3.

- [ ] **Step 3: Alternativas de la misma fase, en serie**

Solo si el step 2 rechazó HSEmotion. Primero el ONNX MobileFaceNet FER de OpenCV Zoo, 4,79 MB, misma caja de landmarks, 1 Hz, 2 hilos. Si OpenCV no carga ese ONNX, o el RSS o el audio fallan el mismo criterio, probar el archivo tflite Mini-Xception de 91 KB. Copiar el archivo. No instalar el paquete pip `fer`. Si las dos fallan el criterio, dejar la heurística de la fase 2 (o el lineal, si estaba cableado) y cerrar el plan. No probar B2, MobileViT, FERPlus, Hailo ni ncnn.

- [ ] **Step 4: Cablear un solo modelo si pasó**

Dentro de `_process_tasks_frame`, después de tener landmarks, a 1 Hz, no a 5. El landmarker de Tasks sigue siendo el único detector. La histéresis de `AnimoFacial` envuelve la etiqueta del modelo para que un frame suelto no alargue el silencio. Borrar el script del camino de arranque: se corre a mano.

Agregar a `requirements.txt` solo el intérprete que la medición ya importó con éxito. Nada de `deepface`, `torch`, `timm` ni `fer`.

- [ ] **Step 5: Qué mirar en la Pi**

Las cuatro situaciones, más el minuto de audio. Hablar con la boca abierta no debe quedar en `sorprendido` más que la fase 2. Si el modelo de píxeles vuelve a ese error, descablearlo aunque la latencia haya sido baja: en niños ese fallo está medido y no se acepta a cambio de milisegundos.

- [ ] **Step 6: Criterio de parada**

Esta es la última fase de modelos. No hay fase 6 de otra red.

- [ ] **Step 7: Commit**

Solo si un modelo pasó el step 2 o el 3 y quedó cableado. El script de medición puede commitearse igual, sin los pesos, si no descarga nada al importar.

```bash
git add ProyectoParaRasperrypiV5/bench_emocion_litert.py
git commit -m "Mide un modelo chico de ánimo en LiteRT sin sumarlo al arranque."
```

---

### Task 7: Resolución del landmarker — solo si la etiqueta ya es correcta y la Pi se ahoga

**Files:**
- Modify: `ProyectoParaRasperrypiV5/camera_emotion.py` (función pura del tamaño)
- Modify: `ProyectoParaRasperrypiV5/workers.py` (`_process_tasks_frame`, sin cambiar `apply_camera_capture_size`)
- Test: `ProyectoParaRasperrypiV5/test_camera_emotion.py`

**Produces:** El array que entra al Face Landmarker tiene el lado largo en 480 px. La captura MJPG sigue pidiendo el máximo del driver. El bucle de preview sigue a `frame_rate` 5. La inferencia de emoción, si el audio se ahoga, pasa a 2 Hz.

No empezar esta tarea si la fase 3 (o la 4, si se llegó) todavía no tiene la etiqueta bien. No empezarla si el audio va bien.

**Interfaces:**
- Produces: `marco_landmarker(ancho: int, alto: int, lado: int = 480) -> tuple[int, int]`

- [ ] **Step 1: Test del tamaño**

```python
class TestMarcoLandmarker(unittest.TestCase):
    def test_el_lado_largo_queda_en_480_y_conserva_la_proporcion(self) -> None:
        from camera_emotion import marco_landmarker

        ancho, alto = marco_landmarker(1280, 720, lado=480)
        self.assertEqual(max(ancho, alto), 480)
        self.assertLess(abs((ancho / alto) - (1280 / 720)), 0.02)

    def test_un_frame_chico_no_se_agranda(self) -> None:
        from camera_emotion import marco_landmarker

        self.assertEqual(marco_landmarker(320, 240, lado=480), (320, 240))
```

- [ ] **Step 2: Verlo fallar, implementar, verlo pasar**

`marco_landmarker` escala por `lado / max(ancho, alto)` solo si el lado largo supera `lado`. En `_process_tasks_frame`, redimensionar una copia para `detect_for_video`. Los landmarks vienen normalizados: la caja se dibuja sobre el frame original. No llamar a `apply_camera_capture_size` con 480. Ese helper sigue en 10000×10000 y MJPG.

Si hace falta aliviar CPU, publicar emoción como máximo cada 0,5 s (`INTERVALO_ANIMO_S = 0.5`) sin cambiar `self.frame_rate`. El test `test_emocion_encendida_a_5_fps` sigue en verde.

```text
python -m unittest test_camera_emotion.py -v
```

Expected: PASS.

- [ ] **Step 3: Qué mirar en la Pi**

La línea de captura sigue mostrando el tamaño real del driver, no 480×algo forzado por `apply_camera_capture_size`. Las cuatro situaciones de la fase 3 se mantienen. El overflow de audio que motivó esta tarea baja o desaparece. Si la etiqueta empeora al bajar el detalle, volver el lado a 480 no es obligatorio: dejar el tamaño anterior y anotar que el alivio de CPU no compensó.

- [ ] **Step 4: Commit**

```bash
git add ProyectoParaRasperrypiV5/camera_emotion.py ProyectoParaRasperrypiV5/workers.py ProyectoParaRasperrypiV5/test_camera_emotion.py
git commit -m "El landmarker de ánimo mira un lado largo de 480 px si la Pi se ahoga."
```

---

## Conclusión

El ánimo se arregla en el postproceso de los blendshapes que el Face Landmarker ya entrega. Si el log de la Pi dice FaceMesh, ese camino no publica emoción: se deja solo Tasks y no se corren los dos. La fórmula nueva exige coactivación, niega la sorpresa cuando la boca se abre sin ojos ni cejas, suaviza con EMA e histéresis, resta el neutro de la sesión y anuncia el neutral con la activación débil, no con su complemento. Si en la cámara del peluche la sonrisa, la tristeza y el enojo se separan y hablar deja de verse como sorpresa, se para: no hay CNN. Solo si esa puerta falla se entrena un lineal en NumPy con clips de esta cámara, con la boca abierta etiquetada como neutral. HSEmotion-B0 en LiteRT va después, a 1 Hz y con el recorte de los landmarks ya calculados, y se mide en esta Pi; si XNNPACK rechaza el grafo o el audio se corta, no se adopta. MobileFaceNet y el tflite de 91 KB son el plan B de esa misma fase. No se usan FER2013, AffectNet, RAF-DB, DeepFace, PyTorch ni el paquete fer. Los silencios de 1,2 s y 3,0 s y el landmarker de manos no se tocan. Bajar el lado del landmarker a unos 480 px es un ajuste de CPU para cuando la etiqueta ya sea correcta y la Pi se ahogue.
