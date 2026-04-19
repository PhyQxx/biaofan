/**
 * 离线草稿状态管理
 * 使用 uni.setStorageSync 存储，MVP 阶段无需 SQLite
 */
import { defineStore } from 'pinia'
import api from '../api'
import { checkNetwork } from '../common/utils'

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
      if (pendingDrafts.length === 0) return { success: 0, failed: 0, skipped: 0 }

      // Check network before starting sync loop
      const isOnline = await checkNetwork()
      if (!isOnline) {
        console.log('[Draft] 离线状态，跳过同步')
        return { success: 0, failed: 0, skipped: pendingDrafts.length }
      }
      
      // 按 executionId 分组
      const grouped = {}
      pendingDrafts.forEach(d => {
        if (!grouped[d.executionId]) {
          grouped[d.executionId] = []
        }
        grouped[d.executionId].push(d)
      })
      
      // 逐个 executionId 同步
      let success = 0
      let failed = 0
      for (const [executionId, steps] of Object.entries(grouped)) {
        // Check network before each sync call
        if (!await checkNetwork()) {
          failed += steps.length
          continue
        }
        try {
          // 先上传图片，获取远程 URL；图片上传失败的步骤跳过，保留重试
          const uploadResults = await Promise.all(steps.map(async (step) => {
            let photoUrl = ''
            let uploadFailed = false
            if (step.photoLocalPath) {
              try {
                const uploadRes = await api.upload.image(step.photoLocalPath)
                photoUrl = uploadRes.data.url
              } catch (e) {
                console.error('图片上传失败:', e)
                uploadFailed = true
              }
            }
            return { step, photoUrl, uploadFailed }
          }))
          
          // 分离上传成功和失败的步骤
          const successSteps = uploadResults.filter(r => !r.uploadFailed)
          const failedSteps = uploadResults.filter(r => r.uploadFailed)
          
          // 只有上传成功的步骤才调用同步接口
          if (successSteps.length > 0) {
            const stepsWithUrls = successSteps.map(r => ({
              stepIndex: r.step.stepIndex,
              photoUrl: r.photoUrl,
              note: r.step.note,
              localTimestamp: r.step.localTimestamp
            }))
            
            await api.draft.sync({
              executionId: parseInt(executionId),
              steps: stepsWithUrls
            })
            
            // 标记上传成功的步骤为已同步
            successSteps.forEach(r => {
              r.step.synced = true
            })
          }
          
          // 图片上传失败的步骤增加重试计数，不标记synced，保留重试
          failedSteps.forEach(r => {
            r.step.retryCount = (r.step.retryCount || 0) + 1
          })
          
          const syncedCount = successSteps.length
          success += syncedCount
          if (syncedCount > 0) {
            uni.showToast({ title: `已同步 ${syncedCount} 条草稿`, icon: 'success' })
          } else {
            uni.showToast({ title: '图片上传失败，待重试', icon: 'none' })
          }
        } catch (e) {
          console.error('草稿同步失败:', e)
          failed += steps.length
          // 增加重试计数
          steps.forEach(step => {
            step.retryCount = (step.retryCount || 0) + 1
          })
        }
      }

      this.save()
      return { success, failed, skipped: 0 }
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
