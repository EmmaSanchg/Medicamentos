# 💊 Medicamentos - Asistente de Lectura Accesible

**Medicamentos** es una aplicación de Android diseñada para ayudar a personas con discapacidad visual a identificar cajas de medicinas de forma rápida y sencilla. Utiliza Inteligencia Artificial (Google ML Kit) para reconocer texto en tiempo real y convertirlo en voz.



## 🚀 Características principales
- **Reconocimiento de texto (OCR):** Detecta automáticamente el nombre y detalles de las cajas de medicamentos con la cámara.
- **Feedback Háptico:** El teléfono vibra (1s encendido / 1s apagado) mientras detecta texto, indicando al usuario que está enfocando correctamente.
- **Interfaz Basada en Gestos:** Diseñada para ser usada sin necesidad de ver la pantalla.
- **Ahorro de Batería:** Los procesos de cámara y vibración se detienen automáticamente cuando la app pasa a segundo plano.

## 🛠️ Tecnologías utilizadas
- **Lenguaje:** Kotlin
- **Interfaz:** Jetpack Compose
- **IA/Visión:** Google ML Kit (Text Recognition)
- **Cámara:** CameraX
- **Voz:** Text-to-Speech (TTS) de Android

## 🎮 Cómo usar la aplicación
La app está diseñada para ser operada mediante gestos táctiles en cualquier parte de la pantalla:

1. **Abrir la app:** Di *"Hey Google, abre Medicamentos"* (gracias a los Atajos de Voz configurados). Al abrirse, la app dirá: *"Medicamentos lista"*.
2. **Detectar:** Mueve la cámara frente a la caja. Si el teléfono **vibra**, significa que está detectando texto.
3. **Escuchar (Un toque):** Toca la pantalla una vez para que la app lea todo el texto detectado en voz alta (velocidad pausada de 0.9x).
4. **Detener (Dos toques):** Toca la pantalla dos veces rápidamente para silenciar la voz si ya escuchaste lo que necesitabas.

## 📦 Instalación y Configuración
Si quieres compilar este proyecto por tu cuenta:
1. Clona el repositorio: `git clone https://github.com/TU_USUARIO/Madicamentos.git`
2. Abre el proyecto en **Android Studio (Ladybug o superior)**.
3. Asegúrate de tener instalado el SDK de Android para el nivel de API 34 o superior.
4. Sincroniza el proyecto con los archivos Gradle.
5. Ejecuta en un dispositivo físico (recomendado para probar la vibración y cámara).

---
*Desarrollado como una herramienta de accesibilidad para mejorar la autonomía en el manejo de la salud.*