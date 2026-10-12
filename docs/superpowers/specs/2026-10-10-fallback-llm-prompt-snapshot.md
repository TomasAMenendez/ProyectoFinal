# Snapshot del prompt de TEO (2026-10-10)

Copia del prompt del 3B de la Pi en `ProyectoParaRasperrypiV5/fallback_llm.py`. Sirve para volver atrás ese texto. Gemma 3 en la PC usa `_GEMMA_SYSTEM_PROMPT` (otro texto, con un ejemplo por acción). El contexto de la PC quedó en 8192 (`PC_LLM_N_CTX=2048` lo vuelve a dejar como estaba). La ficha del nene se agrega a los dos prompts.

## Modelos

| Dónde | Modelo | Contexto |
|---|---|---|
| Pi, default | `Llama-3.2-3B-Instruct-Q4_K_M` (`LLM_PROFILE=3b`) | `n_ctx` default **1024** (`LLM_N_CTX=2048` para volver al contexto largo) |
| Pi, perfiles | `1b` descartado por calidad; `8b` es Llama 3.1 8B | mismo helper `llm_n_ctx()` |
| PC | `google_gemma-3-12b-it-Q5_K_M` | `PC_LLM_N_CTX` default **8192** (antes 2048) |

La Pi arma los `messages` y se los manda al servidor. El servidor no tiene un prompt propio.

## Cómo se arma el pedido

`FallbackLLM._build_messages` (`fallback_llm.py`):

1. `_SYSTEM_PROMPT` fijo.
2. Si el perfil del nene está completo, un salto de línea y `build_prompt_section` (`child_profile.py`).
3. Si hay emoción por encima del umbral, una línea `Contexto: ...`.
4. Historial.
5. Lo que acaba de decir el nene, rol `user`.

Historial (`conversation_memory.py`, cambio del 2026-10-10):

- Se guardan **10 turnos** (20 mensajes).
- Al servidor de PC se le mandan esos 10.
- Si responde el 3B de la Pi, `local_history()` recorta a **2 turnos** (4 mensajes).

Muestreo del 3B en `complete_sync`: `max_tokens=80`, `temperature=0.70`, `top_p=0.9`, `top_k=40`, `repeat_penalty=1.15`. Stop tokens de Llama (`<|eot_id|>`, headers, `<|im_end|>`).

## Texto fijo de hoy

Pegado tal cual está en la constante `_SYSTEM_PROMPT`. Las comillas de Python se concatenan en **un solo párrafo**.

```
Tu nombre es TEO. Sos un peluche que habla con nenes de 3 a 7 años. Primera persona; vos, mirá, dale. Nunca tercera persona ni 'el nene', 'el usuario' o 'el LLM'. Contestá lo que dijo el nene en 1 o 2 oraciones, máximo 25 palabras. Si no se entiende, preguntá con alegría. No inventes países ni datos. Si no sabés, decí no sé. Cuentas simples: da el resultado. Sin emojis, comillas ni asteriscos. No juegues vos ni inventes reglas. Si pide adivinar el objeto, contar, sombras o Piedra Papel o Tijera: [PLAY_GAME:sombras] o contar, adivina, piedra. El corchete no se dice. No ofrezcas cuento ni canción si no lo pidió. No inventes títulos. Si pide cuento: [LIST_STORIES] solo, sin texto. Si pide música y querés listar: [LIST_MUSIC] solo, sin texto. [PLAY_STORY:título] para leer uno de la lista. [PLAY_MUSIC] o [PLAY_MUSIC:nombre] para una canción. [STOP_MUSIC] para parar. Corchetes AL FINAL y no se dicen. No escribas la palabra Tags. No inventes tags. [INTENTS_OFF] si preguntás y esperás respuesta. [INTENTS_ON] obligatorio cuando dejás de preguntar. Si pide a mamá/papá: [NOTIFY_PARENT:razón explicandole al padre] y [INTENTS_ON]. [NOTIFY_PARENT:razón explicandole al padre] SOLO si pide a mamá/papá, miedo, crisis o duele de verdad. NO uses [NOTIFY_PARENT] por una palabra suelta, un color, un juego, una verdura, un bicho o charla de jardín. Si el juego se llama mamá y papá, no es pedir a los padres. Cara: [EXPRESSION:feliz] [EXPRESSION:triste] [EXPRESSION:sorprendido] [EXPRESSION:enojado]. [CELEBRATE:qué hizo] breve, no vacío. [CALM_MODE] si tiene sueño, está cansado o si el nene se despide. [GO_TO_SLEEP] solo si pide apagarte o irte a dormir, o si confirma que duermas. Si quiere jugar o dice no a otra cosa, no uses [GO_TO_SLEEP].
```

La ficha, si hay perfil, se agrega así (`child_profile.build_prompt_section`):

```
Datos del nene (dalo por cierto; no lo contradigas ni lo recites de una):
Se llama {nombre}. {Es nene. | Es nena. | vacío}Tu disfraz es un {disfraz}; te ve así.
{Gustos: ... si hay}
{El padre pide que sepas: ... si hay}Usá el nombre. {Tratá en masculino.|Tratá en femenino.} No expliques estas notas.
```

## Qué hace el código después de la respuesta

En `complete_sync`, en este orden:

1. `_clean_response`: saca `*acciones*`, paréntesis, emojis y comillas. **No** corta a 25 palabras (un tag largo al inicio se partiría).
2. `drop_unsolicited_story_offer`: si el nene no pidió cuento, borra oraciones que ofrecen cuento. El comentario del código dice que el 3B copia la plantilla del prompt.
3. `drop_prompt_leak`: si la respuesta entera es una palabra del prompt (`veo veo`, `me llamo teo`, `intents_on`, `feliz`, …), la reemplaza por `Contame más, ¿qué pasó?`.
4. `scrub_veo_veo_speech`: cambia «veo veo» por «adivinar el objeto».

En `workers.py`, `_parse_action_tags` acepta solo estos nombres, con argumento opcional después de `:`:

`PLAY_MUSIC`, `STOP_MUSIC`, `NOTIFY_PARENT`, `EXPRESSION`, `CELEBRATE`, `CALM_MODE`, `INTENTS_OFF`, `INTENTS_ON`, `GO_TO_SLEEP`, `LIST_STORIES`, `LIST_MUSIC`, `PLAY_STORY`, `PLAY_GAME`.

Cualquier otro corchete se tira antes del TTS. El texto que se habla es lo que queda sin tags.

## Cómo lo recibe cada modelo

**Llama 3.2 3B.** `llama-cpp` aplica la plantilla de chat de Llama 3: un turno `system` de verdad, con `<|start_header_id|>system<|end_header_id|>`. El párrafo de arriba entra ahí entero. Meta documenta ese formato en la model card de Llama 3.2.

**Gemma 3 12B.** La plantilla oficial de `gemma-3-12b-it` **no** tiene turno `system`. Si el primer mensaje es `system`, lo pega al principio del **primer** turno `user`, con una línea en blanco, y el resto tiene que alternar `user` / `model`. Lo que el servidor cree que es «system» Gemma lo lee como el comienzo de lo que dijo el nene en el primer turno.

Fuente: [Gemma prompt structure](https://ai.google.dev/gemma/docs/core/prompt-structure) y la plantilla `chat_template.jinja` de Gemma 3 12B IT en Hugging Face.

## Qué se usa en otros proyectos para los tags

Meta, para Llama 3.2, no usa `[TAG:valor]`. El formato que entrenaron para herramientas es una llamada entre corchetes, con nombre y argumentos:

`[func_name(param=valor)]`

La instrucción que publican dice: definí las funciones en el system prompt, y si vas a llamar una, devolvé **solo** esa llamada, sin otro texto. Receta de Hugging Face / model card de Llama 3.2: [Llama 3.2 prompt formats](https://developer.meta.com/ai/docs/model-cards-and-prompt-formats/llama3_2/) y el notebook [tool_calling](https://github.com/huggingface/huggingface-llama-recipes/blob/main/tool_calling/tool_calling.ipynb).

Eso **no** se puede copiar tal cual. TEO tiene que hablar y, además, emitir tags. «Solo la llamada, sin otro texto» dejaría al TTS mudo.

Gemma no fue entrenada con nuestros corchetes. FunctionGemma (otro modelo, 270M) usa tokens propios (`<start_function_call>`) y una frase fija de system para activar las herramientas. No aplica a `gemma-3-12b-it`.

Lo que sí coincide con lo que ya vemos en este repo: el 3B **copia** frases del system prompt (`drop_prompt_leak`, `drop_unsolicited_story_offer`). Un ejemplo largo dentro del prompt se lo aprende como respuesta.

## Cómo se lo describiría mejor (no aplicado)

Dejar el párrafo de personalidad corto. Poner los tags en una lista, cada uno con nombre exacto, cuándo usarlo y qué va después de los dos puntos. Una sola línea de ejemplo, marcada como formato, no como diálogo:

```
Hablá en 1 o 2 oraciones.
Si hace falta una acción, agregala AL FINAL, en su propia línea, y no la digas.
Formato: [NOMBRE] o [NOMBRE:dato]
Acciones:
- [PLAY_GAME:sombras|contar|adivina|piedra] solo si pidió ese juego.
- [LIST_STORIES] solo, sin otra frase, si pidió un cuento.
...
Ejemplo de forma, no lo repitas: Qué lindo. [EXPRESSION:feliz]
```

No meter en el prompt las frases que el 3B ya copia (`Me llamo TEO`, `VEO VEO`, la palabra `Neutral`). Esas ya se filtran después, pero siguen ocupando contexto.

En la Pi, `n_ctx` 1024 no da para un prompt más largo más 10 turnos. El recorte a 2 turnos del 3B hay que mantenerlo si el prompt crece. En la PC, 2048 alcanza para 10 turnos y una lista de tags un poco más clara.
