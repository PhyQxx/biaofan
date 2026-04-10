<template>
  <div class="page-header">
    <h2 class="page-title">📈 成长规则配置</h2>
      <button class="btn-primary" @click="handleSaveAll" :disabled="saving">
        {{ saving ? '保存中...' : '💾 保存全部' }}
      </button>
    </div>

    <div v-if="loading" class="loading-state">加载中...</div>
    <template v-else>

      <!-- Segment Thresholds -->
      <div class="config-section">
        <div class="section-header">
          <h3>🏅 段位阈值配置</h3>
          <p class="section-desc">配置各段位的最低经验值要求</p>
        </div>
        <div class="card">
          <table class="data-table">
            <thead>
              <tr>
                <th>段位</th>
                <th>段位图标</th>
                <th>最低经验值（EXP）</th>
                <th>颜色预览</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="seg in segments" :key="seg.segment">
                <td>
                  <span class="segment-tag" :class="'seg-' + seg.segment">
                    {{ seg.segment }}
                  </span>
                </td>
                <td><span class="seg-icon">{{ segmentIcon(seg.segment) }}</span></td>
                <td>
                  <div class="input-inline">
                    <input
                      v-model.number="seg.min_exp"
                      type="number"
                      class="form-input inline-input"
                      min="0"
                    />
                    <span class="unit">EXP</span>
                  </div>
                </td>
                <td>
                  <div class="color-preview" :style="{ background: segmentColor(seg.segment) }"></div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Level EXP Table -->
      <div class="config-section">
        <div class="section-header">
          <h3>⬆️ 等级经验值配置</h3>
          <p class="section-desc">设置每个等级所需的累计经验值（Lv1 ~ Lv99）</p>
        </div>
        <div class="card">
          <div class="level-hint">提示：经验值应递增排列，后一级应大于等于前一级</div>
          <table class="data-table">
            <thead>
              <tr>
                <th>等级</th>
                <th>段位</th>
                <th>所需累计 EXP</th>
                <th>距上一级</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(lv, idx) in levelExp" :key="lv.level">
                <td class="level-cell">
                  <span class="level-badge" :class="'level-' + lv.segment">
                    Lv.{{ lv.level }}
                  </span>
                </td>
                <td>
                  <span class="segment-tag sm" :class="'seg-' + lv.segment">{{ lv.segment }}</span>
                </td>
                <td>
                  <div class="input-inline">
                    <input
                      v-model.number="lv.exp_required"
                      type="number"
                      class="form-input inline-input exp-input"
                      min="0"
                      @change="validateLevelExp(idx)"
                    />
                    <span class="unit">EXP</span>
                  </div>
                </td>
                <td class="diff-cell">
                  <span v-if="idx > 0" class="diff-tag">
                    +{{ Math.max(0, lv.exp_required - levelExp[idx - 1].exp_required) }}
                  </span>
                  <span v-else class="text-muted">—</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="levelExp.length > 0" class="level-summary">
            共 {{ levelExp.length }} 个等级 | 最高所需 EXP：{{ levelExp[levelExp.length - 1]?.exp_required || 0 }}
          </div>
        </div>
      </div>

      <!-- Badge Score Rules -->
      <div class="config-section">
        <div class="section-header">
          <h3>🎁 徽章积分奖励规则</h3>
          <p class="section-desc">配置各徽章的 EXP 和积分奖励（用户获得徽章时自动发放）</p>
        </div>
        <div class="card">
          <table class="data-table">
            <thead>
              <tr>
                <th>徽章</th>
                <th>条件</th>
                <th>EXP 奖励</th>
                <th>积分奖励</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="rule in scoreRules" :key="rule.badge_id">
                <td class="name-cell">{{ rule.rule_name }}</td>
                <td class="text-muted">{{ rule.badge_id }}</td>
                <td>
                  <div class="input-inline">
                    <input
                      v-model.number="rule.exp_reward"
                      type="number"
                      class="form-input inline-input sm-input"
                      min="0"
                    />
                    <span class="unit">EXP</span>
                  </div>
                </td>
                <td>
                  <div class="input-inline">
                    <input
                      v-model.number="rule.score_reward"
                      type="number"
                      class="form-input inline-input sm-input"
                      min="0"
                    />
                    <span class="unit">积分</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

    </template>
</template>

<script setup lang="ts">


/**
 * 管理后台 - 积分规则配置页
 * - 配置各项行为的积分奖励规则
 * - 保存规则
 */
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getGrowthRules,
  updateGrowthRules,
  type LevelRule,
  type SegmentRule,
  type ScoreRule,
} from '@/api/admin'

const loading = ref(false)
const saving = ref(false)

const levelExp = ref<LevelRule[]>([])
const segmentThreshold = ref<SegmentRule[]>([])
const scoreRules = ref<ScoreRule[]>([])

// Computed segments list for display
const segments = segmentThreshold

const segmentIcon = (seg: string) => ({
  bronze: '🥉', silver: '🥈', gold: '🥇', diamond: '💎', king: '👑',
}[seg] || seg)

const segmentColor = (seg: string) => ({
  bronze: '#CD7F32',
  silver: '#C0C0C0',
  gold: '#FFD700',
  diamond: '#B9F2FF',
  king: '#FF6B35',
}[seg] || '#8B90A0')

const loadData = async () => {
  loading.value = true
  try {
    const resp = await getGrowthRules()
    levelExp.value = resp.level_exp || []
    segmentThreshold.value = resp.segment_threshold || []
    scoreRules.value = resp.score_rule || []
  } catch {
    ElMessage.error('加载成长规则失败')
  } finally {
    loading.value = false
  }
}

const validateLevelExp = (idx: number) => {
  if (idx > 0 && levelExp.value[idx].exp_required < levelExp.value[idx - 1].exp_required) {
    ElMessage.warning(`Lv.${levelExp.value[idx].level} 的经验值不能小于 Lv.${levelExp.value[idx - 1].level}`)
  }
}

const handleSaveAll = async () => {
  saving.value = true
  try {
    await updateGrowthRules({
      level_exp: levelExp.value,
      segment_threshold: segmentThreshold.value,
      score_rule: scoreRules.value,
    })
    ElMessage.success('成长规则保存成功')
    loadData()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-title { font-size: 20px; font-weight: 600; color: #E8EAF0; }
.loading-state { text-align: center; color: #8B90A0; padding: 60px 0; }
.config-section { margin-bottom: 32px; }
.section-header { margin-bottom: 12px; }
.section-header h3 { font-size: 16px; font-weight: 600; color: #E8EAF0; }
.section-desc { font-size: 13px; color: #8B90A0; margin-top: 4px; }
.card { background: #1A1D27; border: 1px solid #2D3348; border-radius: 12px; padding: 20px; }
.level-hint { font-size: 12px; color: #8B90A0; background: #22263A; border-radius: 6px; padding: 8px 12px; margin-bottom: 16px; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { text-align: left; font-size: 12px; font-weight: 600; color: #8B90A0; padding: 8px 12px; border-bottom: 1px solid #2D3348; }
.data-table td { padding: 10px 12px; border-bottom: 1px solid #2D3348; font-size: 14px; color: #E8EAF0; vertical-align: middle; }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: #22263A; }

.segment-tag {
  padding: 4px 12px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  display: inline-block;
}
.segment-tag.sm { font-size: 11px; padding: 2px 8px; }
.seg-bronze { background: rgba(205,127,50,0.15); color: #CD7F32; border: 1px solid #CD7F32; }
.seg-silver { background: rgba(192,192,192,0.15); color: #C0C0C0; border: 1px solid #C0C0C0; }
.seg-gold { background: rgba(255,215,0,0.15); color: #FFD700; border: 1px solid #FFD700; }
.seg-diamond { background: rgba(185,242,255,0.15); color: #B9F2FF; border: 1px solid #B9F2FF; }
.seg-king { background: rgba(255,107,53,0.15); color: #FF6B35; border: 1px solid #FF6B35; }
.seg-icon { font-size: 24px; }
.color-preview { width: 40px; height: 24px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.1); }

.level-cell { white-space: nowrap; }
.level-badge {
  padding: 4px 12px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  font-family: monospace;
}
.level-bronze { background: rgba(205,127,50,0.15); color: #CD7F32; }
.level-silver { background: rgba(192,192,192,0.15); color: #C0C0C0; }
.level-gold { background: rgba(255,215,0,0.15); color: #FFD700; }
.level-diamond { background: rgba(185,242,255,0.15); color: #B9F2FF; }
.level-king { background: rgba(255,107,53,0.15); color: #FF6B35; }

.input-inline { display: flex; align-items: center; gap: 6px; }
.form-input { background: #22263A; border: 1px solid #2D3348; border-radius: 6px; padding: 7px 10px; font-size: 14px; color: #E8EAF0; outline: none; transition: border-color 0.2s; font-family: monospace; }
.form-input:focus { border-color: #5B7FFF; }
.inline-input { width: 100px; }
.exp-input { width: 130px; }
.sm-input { width: 80px; }
.unit { font-size: 12px; color: #8B90A0; white-space: nowrap; }
.diff-cell { color: #4ADE80; font-family: monospace; font-size: 13px; }
.diff-tag { background: rgba(74,222,128,0.1); padding: 2px 6px; border-radius: 4px; }
.text-muted { color: #8B90A0; font-size: 13px; }
.name-cell { font-weight: 600; }
.level-summary { margin-top: 12px; font-size: 12px; color: #8B90A0; text-align: right; }

.btn-primary { background: #5B7FFF; color: #fff; border: none; border-radius: 8px; padding: 10px 20px; font-size: 14px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
.btn-primary:hover { background: #7B9FFF; }
.btn-primary:disabled { background: #2D3348; color: #555A6E; cursor: not-allowed; }
</style>
