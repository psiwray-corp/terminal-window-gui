package com.psiwray.organization.terminal_window_gui.examples

import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11
import java.awt.image.BufferedImage
import java.nio.IntBuffer
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * Captures the current GL front buffer after a frame.
 *
 * The readback uses the owner thread's current context. Image encoding uses
 * headless AWT; no compositor or desktop screenshot tool is involved.
 */
internal fun captureFrontBuffer(path: Path) {
    val viewport = BufferUtils.createIntBuffer(4)
    GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport)
    var width = viewport.get(2)
    var height = viewport.get(3)
    if (width <= 0 || height <= 0) {
        val requestedWidth: IntBuffer = IntBuffer.allocate(1)
        val requestedHeight: IntBuffer = IntBuffer.allocate(1)
        GLFW.glfwGetFramebufferSize(GLFW.glfwGetCurrentContext(), requestedWidth, requestedHeight)
        width = requestedWidth.get(0)
        height = requestedHeight.get(0)
    }
    require(width > 0 && height > 0) { "framebuffer is empty: ${width}x$height" }
    val pixels = BufferUtils.createByteBuffer(width * height * 4)
    GL11.glFinish()
    GL11.glReadBuffer(GL11.GL_BACK)
    GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1)
    GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels)
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            val source = ((height - 1 - y) * width + x) * 4
            val red = pixels.get(source).toInt() and 0xFF
            val green = pixels.get(source + 1).toInt() and 0xFF
            val blue = pixels.get(source + 2).toInt() and 0xFF
            val alpha = pixels.get(source + 3).toInt() and 0xFF
            image.setRGB(x, y, (alpha shl 24) or (red shl 16) or (green shl 8) or blue)
            x++
        }
        y++
    }
    val absolute = path.toAbsolutePath()
    absolute.parent?.let { Files.createDirectories(it) }
    check(ImageIO.write(image, "png", absolute.toFile())) { "PNG writer unavailable" }
}
