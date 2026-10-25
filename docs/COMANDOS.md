# Comandos de VFX

Los comandos se ejecutan localmente en el cliente. Funcionan en mundos individuales y al jugar en un servidor, sin permisos de operador. Los VFX visuales son locales al cliente; la sincronización con otros jugadores aún no está implementada.

## Comandos disponibles

| Comando | Función |
|---|---|
| /vfx o /vfx help | Muestra la ayuda |
| /vfx list | Enumera los VFX, categorías y estados |
| /vfx enable <efecto> | Activa un efecto global |
| /vfx disable <efecto> | Desactiva un efecto y sus instancias colocadas |
| /vfx toggle <efecto> | Alterna entre activo e inactivo |
| /vfx status <efecto> | Consulta el estado de un efecto |
| /vfx disable all | Limpia todos los efectos locales |
| /vfx blackhole spawn | Coloca un agujero negro de diámetro predeterminado de 10 bloques |
| /vfx blackhole spawn <diámetro> | Coloca un agujero negro de tamaño personalizado, de 1 a 100 bloques de diámetro |
| /vfx blackhole clear | Elimina todos los agujeros negros colocados |

Los argumentos sugieren los identificadores registrados al pulsar Tab. También se acepta el nombre corto de los efectos del mod.

## VFX incluidos

- demo_orbit: tres anillos de color alrededor del jugador local. Es un efecto de prueba del motor.
- music_horizon_bars: un ecualizador de neón animado alrededor del horizonte. Las ondas se calculan con frecuencias que cierran exactamente el círculo para evitar una junta visible. Todavía no analiza el audio que se está reproduciendo.
- rainbow_aurora: cortinas semitransparentes ondulantes alrededor del cielo, con cierre continuo y colores arcoíris.
- rainbow_sky: una cúpula translúcida que tiñe el cielo; el matiz cambia de forma gradual a lo largo del arcoíris.
- black_hole: esfera central negra, disco de acreción animado y pase de lente gravitacional que deforma la imagen del mundo alrededor del agujero.

## Ejemplos

- /vfx enable rainbow_sky
- /vfx enable rainbow_aurora
- /vfx enable music_horizon_bars
- /vfx blackhole spawn
- /vfx blackhole spawn 20
- /vfx blackhole clear
- /vfx disable all

Al apuntar a un bloque, el agujero negro aparece frente a la superficie señalada. Si no hay un objetivo seleccionado, aparece a 16 bloques delante de la cámara del jugador. El comando no modifica ni destruye los bloques del mundo: la curvatura es visual y local a tu cliente.

## Añadir un efecto nuevo a la lista

1. Crear una implementación de VfxEffect para su estado y ciclo de vida.
2. Elegir una categoría en VfxEffectCategory.
3. Si se dibuja en el mundo en una coordenada concreta, crear un ClientWorldVfxRenderer y registrarlo en ClientWorldVfxRendererRegistry.
4. Si necesita deformar la imagen completa, añadir un pase GLSL de posprocesamiento y conectarlo al punto adecuado del renderizado.
5. Registrar la definición en ClientVfxCatalog y probarla en el cliente real.

El listado y las sugerencias se generan desde el registro central.
