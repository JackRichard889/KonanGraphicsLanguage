package dev.jackrichard.kana

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES32
import javax.microedition.khronos.opengles.GL10

actual class KanaContext {
    lateinit var delegateView: GL10

    actual fun queueUp(func: KanaCommandBuffer.() -> Unit) {
        val kglBuffer = KanaCommandBuffer()
        kglBuffer.func()
    }

    actual class KanaCommandBuffer internal constructor() {
        private var pipeline: KanaPipeline? = null
        actual fun linkPipeline(pipeline: KanaPipeline) {
            // TODO: implement with platform specific code
            this.pipeline = pipeline
        }
    }
}

@SuppressLint("StaticFieldLeak")
actual object KanaGlobals {
    lateinit var context: Context
}

actual class KanaTexture private constructor(name: String, ext: String) {
    init {

    }

    actual companion object {
        actual fun genNew(name: String, extension: String) = KanaTexture(name, extension)
    }
}

/*actual class KGLModel actual constructor(source: KGLAsset) {
    private var raw: GLESMesh

    init {
        raw = GLESMesh(KGLGlobals.context, "${source.name}.${source.extension}")
    }
}*/

actual class KanaShaderSource { var shader: Int = 0 }

actual class KanaPipeline private actual constructor() {
    private var program: Int = 0
    private var vertexDescriptor: VertexDescriptor? = null

    actual companion object {
        actual fun initNew() : KanaPipeline = KanaPipeline().apply {
            program = GLES32.glCreateProgram()
        }
    }

    actual fun setVertexFunction(shader: Pair<KanaShader?, KanaShader?>) { GLES32.glAttachShader(program, (if (shader.first == null) shader.second else shader.first)!!.compiledSource.shader) }
    actual fun setFragmentFunction(shader: Pair<KanaShader?, KanaShader?>) { GLES32.glAttachShader(program, (if (shader.first == null) shader.second else shader.first)!!.compiledSource.shader) }
    actual fun setVertexDescriptor(descriptor: VertexDescriptor) {
        vertexDescriptor = descriptor

        fun calcOffset(indexFrom: Int) : Int {
            if (indexFrom < 0) { return 0 }
            return descriptor.elements.subList(0, indexFrom).sumOf { it.size }
        }

        val descriptorSize: Int = descriptor.elements.sumOf { it.size }

        descriptor.elements.zip(0 until descriptor.elements.size).forEach {
            val identifier = GLES32.glGetAttribLocation(program, it.first.name)

            GLES32.glEnableVertexAttribArray(identifier)
            GLES32.glVertexAttribPointer(
                identifier,
                it.first.size,
                when (it.first.type.simpleName) {
                    "Vec2" -> GLES32.GL_FLOAT
                    "Vec3" -> GLES32.GL_FLOAT
                    "Vec4" -> GLES32.GL_FLOAT
                    else -> 0
                },
                false,
                descriptorSize,
                calcOffset(it.second - 1)
            )
        }
    }

    fun deInitFromDescriptor() {
        // TODO: this needs to be called somewhere
        vertexDescriptor!!.elements.forEach {
            val identifier = GLES32.glGetAttribLocation(program, it.name)
            GLES32.glDisableVertexAttribArray(identifier)
        }
    }
}

actual class KanaShader private actual constructor(val platform: KanaPlatform, val source: String, val type: KanaShaderType, val name: String) {

    actual var compiledSource: KanaShaderSource = KanaShaderSource().apply {
        shader = GLES32.glCreateShader(
            when (type) {
                KanaShaderType.FRAGMENT -> GLES32.GL_FRAGMENT_SHADER
                KanaShaderType.VERTEX -> GLES32.GL_VERTEX_SHADER
                else -> 0
            }
        ).also { shader1 ->
            GLES32.glShaderSource(shader1, source)
            GLES32.glCompileShader(shader1)
        }
    }

    actual companion object {
        actual fun compileShader(
            platform: KanaPlatform,
            type: KanaShaderType,
            name: String,
            source: String
        ): KanaShader? = if (platform == KanaPlatform.ANDROID) KanaShader(platform, source, type, name) else null
    }

}