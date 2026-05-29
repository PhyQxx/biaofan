<template>
  <div class="knowledge-assistant-wrapper" :class="{ expanded: isOpen }">
    <!-- Toggle Button -->
    <div class="assistant-toggle" @click="isOpen = !isOpen">
      <span class="icon">🎓</span>
      <span class="label" v-if="!isOpen">组织知识助手</span>
      <span class="close" v-else>✕</span>
    </div>

    <!-- Chat Container -->
    <div class="assistant-container" v-if="isOpen">
      <div class="chat-header">
        <h3>🎓 组织知识助手</h3>
        <p>基于本组织 SOP 库为您解答</p>
      </div>

      <div class="chat-body" ref="chatBody">
        <div 
          v-for="(msg, index) in messages" 
          :key="index" 
          class="message" 
          :class="msg.role"
        >
          <div class="message-bubble">
            <template v-if="msg.loading">
              <span class="dot-typing"></span>
            </template>
            <template v-else>
              {{ msg.content }}
            </template>
          </div>
        </div>
      </div>

      <div class="chat-footer">
        <input 
          v-model="input" 
          class="chat-input" 
          placeholder="问问组织内的规范..." 
          @keyup.enter="handleSend"
          :disabled="loading"
        />
        <button class="btn-send" @click="handleSend" :disabled="loading || !input.trim()">
          发送
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getOrgQa } from '@/api/ai'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const currentOrgId = computed(() => authStore.currentOrgId)

const isOpen = ref(false)
const input = ref('')
const loading = ref(false)
const chatBody = ref<HTMLElement | null>(null)

interface Message {
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
}

const messages = ref<Message[]>([
  { role: 'assistant', content: '您好！我是组织知识助手。您可以询问我关于本组织 SOP 规范的任何问题。' }
])

async function handleSend() {
  if (!input.value.trim() || loading.value) return
  if (!currentOrgId.value) {
    ElMessage.warning('请先选择一个组织空间')
    return
  }

  const userMsg = input.value
  messages.value.push({ role: 'user', content: userMsg })
  input.value = ''
  
  const aiMsgIndex = messages.value.push({ role: 'assistant', content: '', loading: true }) - 1
  loading.value = true
  scrollToBottom()

  try {
    const res = await getOrgQa({
      orgId: currentOrgId.value,
      question: userMsg
    })
    if (res.code === 200) {
      messages.value[aiMsgIndex].content = res.data
    } else {
      messages.value[aiMsgIndex].content = '抱歉，我遇到了一点问题：' + res.message
    }
  } catch (e) {
    messages.value[aiMsgIndex].content = '网络异常，请稍后再试'
  } finally {
    messages.value[aiMsgIndex].loading = false
    loading.value = false
    scrollToBottom()
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (chatBody.value) {
      chatBody.value.scrollTop = chatBody.value.scrollHeight
    }
  })
}
</script>

<style scoped>
.knowledge-assistant-wrapper {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.assistant-toggle {
  background: #4F46E5;
  color: white;
  height: 48px;
  padding: 0 20px;
  border-radius: 24px;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  box-shadow: 0 10px 25px rgba(79, 70, 229, 0.4);
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.assistant-toggle:hover { transform: scale(1.05); }

.assistant-container {
  width: 360px;
  height: 500px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 15px 50px rgba(0,0,0,0.15);
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #eee;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.chat-header {
  padding: 20px;
  background: #f9fafb;
  border-bottom: 1px solid #eee;
}
.chat-header h3 { margin: 0; font-size: 16px; }
.chat-header p { margin: 4px 0 0 0; font-size: 12px; color: #666; }

.chat-body {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message { display: flex; }
.message.user { justify-content: flex-end; }
.message-bubble {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.5;
}
.user .message-bubble { background: #4F46E5; color: white; border-bottom-right-radius: 2px; }
.assistant .message-bubble { background: #f3f4f6; color: #333; border-bottom-left-radius: 2px; }

/* Markdown Styles */
.markdown-content :deep(p) { margin: 0 0 8px 0; }
.markdown-content :deep(p:last-child) { margin-bottom: 0; }
.markdown-content :deep(h1), .markdown-content :deep(h2), .markdown-content :deep(h3) { 
  margin: 12px 0 8px 0; font-size: 1.1em; font-weight: 600; 
}
.markdown-content :deep(ul), .markdown-content :deep(ol) { 
  margin: 0 0 8px 0; padding-left: 20px; 
}
.markdown-content :deep(code) { 
  background: rgba(0,0,0,0.05); padding: 2px 4px; border-radius: 4px; font-family: monospace; font-size: 0.9em;
}
.markdown-content :deep(pre) {
  background: #2d2d2d; color: #ccc; padding: 12px; border-radius: 8px; overflow-x: auto; margin: 8px 0;
}
.markdown-content :deep(pre code) { background: none; padding: 0; color: inherit; }
.markdown-content :deep(strong) { font-weight: 700; color: #000; }

.chat-footer {
  padding: 16px;
  border-top: 1px solid #eee;
  display: flex;
  gap: 10px;
}
.chat-input {
  flex: 1;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #eee;
  border-radius: 18px;
  outline: none;
  font-size: 14px;
}
.chat-input:focus { border-color: #4F46E5; }
.btn-send {
  background: #4F46E5;
  color: white;
  border: none;
  padding: 0 16px;
  border-radius: 18px;
  cursor: pointer;
  font-weight: 600;
}
.btn-send:disabled { background: #ccc; }

.dot-typing {
  display: inline-block;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #999;
  animation: typing 1s infinite alternate;
}
@keyframes typing {
  from { box-shadow: 8px 0 #999, 16px 0 #ccc; }
  to { box-shadow: 8px 0 #ccc, 16px 0 #999; }
}
</style>
