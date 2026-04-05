/**
 * 离线草稿状态管理
 * 使用 uni.setStorageSync 存储，MVP 阶段无需 SQLite
 */
import { defineStore } from 'pinia'
import api from '../api'

export const useDraftStore = defineStore('draft', {
  state: () => ({
    drafts: uni.getStorageSync('localDrafts') || []
  }),
  
  getters: {
    hasPendingDrafts: (state) => state.drafts.some(d => !d.synced)
  },
  
  actions: {
    /**
     * 添加草稿（离线时调用）
     */
    addDraft(executionId, stepIndex, photoLocalPath, note) {
      const draft = {
        id: this.genUUID(),
        executionId,
        stepIndex,
        photoLocalPath,
        note,
        localTimestamp: new Date().toISOString(),
        synced: false,
        retryCount: 0
      }
      this.drafts.push(draft)
      this.save()
      return draft
    },
    
    /**
     * 同步所有草稿到服务器
     */
    async syncAll() {
      const pendingDrafts = this.drafts.filter(d => !d.synced)
      if (pendingDrafts.length === 0) return
      
      // 按 executionId 分组
      const grouped = {}
      pendingDrafts.forEach(d => {
        if (!grouped[d.executionId]) {
          grouped[d.executionId] = []
        }
        grouped[d.executionId].push(d)
      })
      
      // 逐个 executionId 同步
      for (const [executionId, steps] of Object.entries(grouped)) {
        try {
          // 先上传图片，获取远程 URL
          const stepsWithUrls = await Promise.all(steps.map(async (step) => {
            let photoUrl = ''
            if (step.photoLocalPath) {
              try {
                const uploadRes = await api.upload.image(step.photoLocalPath)
                photoUrl = uploadRes.data.url
              } catch (e) {
                console.error('图片上传失败:', e)
              }
            }
            return {
              stepIndex: step.stepIndex,
              photoUrl,
              note: step.note,
              localTimestamp: step.localTimestamp,
              synced: true
            }
          }))
          
          // 调用同步接口
          await api.draft.sync({
            executionId: parseInt(executionId),
            steps: stepsWithUrls
          })
          
          // 标记为已同步
          steps.forEach(step => {
            step.synced = true
          })
          
          uni.showToast({ title: `已同步 ${steps.length} 条草稿`, icon: 'success' })
        } catch (e) {
          console.error('草稿同步失败:', e)
          // 增加重试计数
          steps.forEach(step => {
            step.retryCount = (step.retryCount || 0) + 1
          })
        }
      }
      
      this.save()
    },
    
    /**
     * 手动触发单条草稿同步
     */
    async syncOne(draftId) {
      const draft = this.drafts.find(d => d.id === draftId)
      if (!draft || draft.synced) return
      
      try {
        let photoUrl = ''
        if (draft.photoLocalPath) {
          const uploadRes = await api.upload.image(draft.photoLocalPath)
          photoUrl = uploadRes.data.url
        }
        
        await api.draft.sync({
          executionId: parseInt(draft.executionId),
          steps: [{
            stepIndex: draft.stepIndex,
            photoUrl,
            note: draft.note,
            localTimestamp: draft.localTimestamp,
            synced: true
          }]
        })
        
        draft.synced = true
        this.save()
        uni.showToast({ title: '同步成功', icon: 'success' })
      } catch (e) {
        draft.retryCount = (draft.retryCount || 0) + 1
        this.save()
        uni.showToast({ title: '同步失败', icon: 'none' })
        throw e
      }
    },
    
    /**
     * 删除草稿
     */
    removeDraft(draftId) {
      const index = this.drafts.findIndex(d => d.id === draftId)
      if (index > -1) {
        this.drafts.splice(index, 1)
        this.save()
      }
    },
    
    /**
     * 保存到本地存储
     */
    save() {
      uni.setStorageSync('localDrafts', this.drafts)
    },
    
    /**
     * 生成 UUID
     */
    genUUID() {
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = Math.random() * 16 | 0
        const v = c === 'x' ? r : (r & 0x3 | 0x8)
        return v.toString(16)
      })
    }
  }
})
