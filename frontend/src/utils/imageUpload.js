const DEFAULT_IMAGE_OPTIONS = {
  maxWidth: 1600,
  maxHeight: 1600,
  quality: 0.78,
  minBytes: 350 * 1024,
  outputType: 'image/jpeg',
  outputExtension: '.jpg'
}

const getImageDimensions = (width, height, maxWidth, maxHeight) => {
  if (width <= maxWidth && height <= maxHeight) {
    return { width, height }
  }
  const ratio = Math.min(maxWidth / width, maxHeight / height)
  return {
    width: Math.max(1, Math.round(width * ratio)),
    height: Math.max(1, Math.round(height * ratio))
  }
}

const replaceExtension = (name, extension) => {
  const baseName = String(name || 'image')
  const dotIndex = baseName.lastIndexOf('.')
  return `${dotIndex > 0 ? baseName.slice(0, dotIndex) : baseName}${extension}`
}

const loadImage = (file) => new Promise((resolve, reject) => {
  const objectUrl = URL.createObjectURL(file)
  const image = new Image()

  image.onload = () => {
    URL.revokeObjectURL(objectUrl)
    resolve(image)
  }

  image.onerror = () => {
    URL.revokeObjectURL(objectUrl)
    reject(new Error('图片解析失败'))
  }

  image.src = objectUrl
})

const compressImageFile = async (file, options = {}) => {
  const config = { ...DEFAULT_IMAGE_OPTIONS, ...options }

  if (!(file instanceof File) || !file.type?.startsWith('image/')) {
    return file
  }

  if (file.type === 'image/gif' || file.size <= config.minBytes) {
    return file
  }

  const image = await loadImage(file)
  const originalWidth = image.naturalWidth || image.width
  const originalHeight = image.naturalHeight || image.height
  const { width, height } = getImageDimensions(
    originalWidth,
    originalHeight,
    config.maxWidth,
    config.maxHeight
  )

  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height

  const context = canvas.getContext('2d', { alpha: false })
  if (!context) {
    return file
  }

  context.drawImage(image, 0, 0, width, height)

  const blob = await new Promise(resolve => {
    canvas.toBlob(resolve, config.outputType, config.quality)
  })

  if (!blob || blob.size >= file.size * 0.95) {
    return file
  }

  return new File(
    [blob],
    replaceExtension(file.name, config.outputExtension),
    {
      type: blob.type,
      lastModified: Date.now()
    }
  )
}

export const compressElementUploadFileList = async (changedFile, fileList, options = {}) => {
  return Promise.all(
    fileList.map(async fileItem => {
      if (!fileItem?.raw || fileItem.uid !== changedFile?.uid) {
        return fileItem
      }

      const compressedFile = await compressImageFile(fileItem.raw, options)
      if (compressedFile === fileItem.raw) {
        return fileItem
      }

      return {
        ...fileItem,
        name: compressedFile.name,
        size: compressedFile.size,
        raw: compressedFile,
        url: URL.createObjectURL(compressedFile)
      }
    })
  )
}
