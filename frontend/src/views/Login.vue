<template>
  <div class="login-shell">
    <div class="login-ambient">
      <div class="ambient-orb ambient-orb--blue"></div>
      <div class="ambient-orb ambient-orb--green"></div>
      <div class="ambient-grid"></div>
    </div>

    <div class="login-panel">
      <section class="brand-panel">
        <div class="brand-panel__eyebrow">Admin Console</div>
        <img :src="sacLogo" alt="Logo" class="brand-panel__logo" />
        <h1 class="brand-panel__title">公务车辆管理系统</h1>
        <p class="brand-panel__desc">
          借还车、加油报备、提醒闭环和版本日志统一在一个管理工作台内完成。
        </p>

        <div class="brand-panel__chips">
          <span class="brand-chip">统一车辆状态</span>
          <span class="brand-chip">闭环提醒可追踪</span>
          <span class="brand-chip">版本日志留痕</span>
        </div>

        <div class="brand-highlights">
          <article class="brand-highlight">
            <div class="brand-highlight__label">调度视图</div>
            <div class="brand-highlight__text">首页统一聚合车辆、提醒和最近借还车动态。</div>
          </article>
          <article class="brand-highlight">
            <div class="brand-highlight__label">闭环留痕</div>
            <div class="brand-highlight__text">借还车现场照片、异常处理和版本更新都在后台可回溯。</div>
          </article>
        </div>
      </section>

      <section class="login-card">
        <div class="login-card__eyebrow">管理端登录</div>
        <h2 class="login-card__title">进入调度工作台</h2>
        <p class="login-card__desc">登录后即可进入管理端查看车辆运行、版本日志和闭环处理状态。</p>

        <el-form ref="formRef" :model="loginForm" :rules="rules" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              size="large"
            />
          </el-form-item>

          <el-form-item>
            <el-input
              v-model="loginForm.password"
              type="password"
              show-password
              placeholder="请输入密码"
              :prefix-icon="Lock"
              size="large"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <div class="login-form__hint">开发模式下部分账号可空密码登录，生产环境建议保留密码。</div>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-button"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '进入系统' }}
            </el-button>
          </el-form-item>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'
import sacLogo from '@/assets/sac-logo.svg'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login(loginForm)
    userStore.setToken(res.token)
    userStore.setUserInfo({
      id: res.userId,
      driverId: res.driverId,
      username: res.username,
      realName: res.realName,
      role: res.role
    })
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-shell {
  position: relative;
  min-height: 100vh;
  padding: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 20% 18%, rgba(63, 131, 248, 0.22), transparent 30%),
    radial-gradient(circle at 82% 24%, rgba(16, 185, 129, 0.18), transparent 26%),
    linear-gradient(135deg, #091427 0%, #0d2240 52%, #13324c 100%);
  overflow: hidden;
}

.login-ambient {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.ambient-orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(10px);
}

.ambient-orb--blue {
  width: 320px;
  height: 320px;
  top: -80px;
  right: 8%;
  background: radial-gradient(circle, rgba(63, 131, 248, 0.48), transparent 68%);
}

.ambient-orb--green {
  width: 280px;
  height: 280px;
  bottom: -70px;
  left: 6%;
  background: radial-gradient(circle, rgba(16, 185, 129, 0.34), transparent 68%);
}

.ambient-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.06) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.48), transparent 88%);
}

.login-panel {
  position: relative;
  z-index: 1;
  width: min(1100px, 100%);
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  border-radius: 32px;
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.28);
}

.brand-panel {
  padding: 48px 50px;
  background:
    radial-gradient(circle at top right, rgba(94, 234, 212, 0.18), transparent 26%),
    linear-gradient(160deg, rgba(8, 20, 39, 0.92) 0%, rgba(14, 34, 63, 0.92) 52%, rgba(17, 48, 70, 0.90) 100%);
  color: #f7fbff;
  display: flex;
  flex-direction: column;
}

.brand-panel__eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: rgba(197, 217, 239, 0.78);
}

.brand-panel__logo {
  width: 92px;
  margin-top: 28px;
}

.brand-panel__title {
  margin-top: 28px;
  font-size: 38px;
  line-height: 1.18;
  font-weight: 800;
}

.brand-panel__desc {
  margin-top: 16px;
  max-width: 420px;
  font-size: 15px;
  line-height: 1.75;
  color: rgba(220, 232, 246, 0.84);
}

.brand-panel__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.brand-chip {
  display: inline-flex;
  align-items: center;
  padding: 10px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #eff7ff;
}

.brand-highlights {
  margin-top: auto;
  display: grid;
  gap: 14px;
  padding-top: 28px;
}

.brand-highlight {
  padding: 16px 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.09);
}

.brand-highlight__label {
  font-size: 13px;
  font-weight: 800;
  color: #ffffff;
}

.brand-highlight__text {
  margin-top: 6px;
  line-height: 1.7;
  color: rgba(219, 229, 241, 0.8);
  font-size: 13px;
}

.login-card {
  padding: 46px 42px;
  background: rgba(249, 252, 255, 0.98);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-card__eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #7a879d;
}

.login-card__title {
  margin-top: 12px;
  font-size: 32px;
  font-weight: 800;
  color: #152239;
}

.login-card__desc {
  margin-top: 10px;
  font-size: 14px;
  line-height: 1.7;
  color: #67768d;
}

.login-form {
  margin-top: 26px;
}

.login-form__hint {
  margin: 4px 0 20px;
  font-size: 12px;
  line-height: 1.6;
  color: #7f8aa0;
}

.login-button {
  width: 100%;
  height: 52px;
  font-size: 15px;
  letter-spacing: 0.08em;
}

@media (max-width: 980px) {
  .login-panel {
    grid-template-columns: 1fr;
  }

  .brand-highlights {
    margin-top: 26px;
  }
}

@media (max-width: 640px) {
  .login-shell {
    padding: 16px;
  }

  .brand-panel,
  .login-card {
    padding: 28px 24px;
  }

  .brand-panel__title,
  .login-card__title {
    font-size: 28px;
  }
}
</style>
