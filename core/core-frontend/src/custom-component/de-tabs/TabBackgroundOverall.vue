<script setup lang="ts">
import BackgroundOverallCommon from '@/components/visualization/component-background/BackgroundOverallCommon.vue'
import { toRefs } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
const emits = defineEmits(['onTitleBackgroundChange'])
const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    themes?: EditorTheme
    element: any
  }>(),
  {
    showStyle: true,
    themes: 'dark'
  }
)

const { element } = toRefs(props)

const onTitleBackgroundChange = (params, paramsName) => {
  // do change
  if (params) {
    element.value.titleBackground[paramsName] = params
  }
  emits('onTitleBackgroundChange', element.value.titleBackground)
}
</script>

<template>
  <div class="tab-title-background">
    <div class="background-label">{{ t('visualization.active_title_background') }}</div>
    <background-overall-common
      :themes="themes"
      :common-background-pop="element.titleBackground.active"
      component-position="component"
      @onBackgroundChange="onTitleBackgroundChange($event, 'active')"
    />
    <div class="background-label">
      <span>{{ t('visualization.inactive_title_background') }}</span>
      <span style="margin: -2px 0 0 24px">
        <el-form-item class="form-item no-margin-bottom" :class="'form-item-' + themes">
          <el-checkbox
            size="small"
            :effect="themes"
            v-model="element.titleBackground.multiply"
            @change="onTitleBackgroundChange(null, null)"
          >
            {{ t('visualization.multiply_active_title_background') }}
          </el-checkbox>
        </el-form-item>
      </span>
    </div>
    <background-overall-common
      v-show="!element.titleBackground.multiply"
      :themes="themes"
      :common-background-pop="element.titleBackground.inActive"
      component-position="component"
      @onBackgroundChange="onTitleBackgroundChange($event, 'inActive')"
    />
  </div>
</template>

<style scoped lang="less">
.tab-title-background {
  width: 100%;
  height: 100%;
}
.background-label {
  font-weight: 500;
  font-size: 12px;
  margin-bottom: 8px;
  display: flex;
}
</style>
