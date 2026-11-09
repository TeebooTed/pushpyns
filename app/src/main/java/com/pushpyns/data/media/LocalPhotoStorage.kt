package com.pushpyns.data.media

import android.net.Uri
import com.pushpyns.domain.repository.CaptureTarget
import com.pushpyns.domain.repository.PhotoStorage
import com.pushpyns.domain.repository.WorkOrderFiles
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalPhotoStorage
@Inject
constructor(private val files: AppFiles) : PhotoStorage {
    override fun prepareCapture(): CaptureTarget {
        val relative = files.allocate("photos", "cam", "jpg")
        return CaptureTarget(relativePath = relative, uri = files.contentUri(relative).toString())
    }

    override suspend fun import(uriString: String): String = files.copyFrom(Uri.parse(uriString), "photos")

    override fun delete(relativePath: String) {
        files.delete(relativePath)
    }

    override fun duplicate(relativePath: String): String = files.duplicate(relativePath, "photos")
}

@Singleton
class AppWorkOrderFiles
@Inject
constructor(private val files: AppFiles) : WorkOrderFiles {
    override fun saveSignature(workOrderId: Long, png: ByteArray): String {
        val relative = "signatures/wo_$workOrderId.png"
        files.write(relative, png)
        return relative
    }

    override fun pdfRelativePath(workOrderId: Long): String = "pdfs/work-order-$workOrderId.pdf"

    override fun exists(relativePath: String): Boolean = files.exists(relativePath)
}
