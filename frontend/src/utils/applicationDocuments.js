import api from '../api'

export async function downloadApplicationDocument(document) {
  if (!document?.id) throw new Error('该记录没有可下载的线上材料')
  const blob = await api.get(`/parking/application-documents/${encodeURIComponent(document.id)}`, {
    responseType: 'blob', timeout: 60000
  })
  const url = URL.createObjectURL(blob)
  const link = window.document.createElement('a')
  link.href = url
  link.download = document.name || '申请材料'
  window.document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export const documentTypeLabels = {
  ID_CARD: '身份证明', PROPERTY_CERT: '房屋证明',
  VEHICLE_LICENSE: '车辆行驶证', RELATIONSHIP_PROOF: '车辆关系证明'
}
