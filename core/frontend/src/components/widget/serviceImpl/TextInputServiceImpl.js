import { WidgetService } from '../service/WidgetService'

const leftPanel = {
  icon: 'iconfont icon-shuru',
  label: 'deinputsearch.label',
  defaultClass: 'text-filter'
}

const dialogPanel = {
  options: {
    attrs: {
      placeholder: 'deinputsearch.placeholder',
      viewIds: [],
      parameters: [],
      fieldId: '',
      dragItems: []

    },
    value: '',
    manualModify: false
  },
  defaultClass: 'text-filter',
  component: 'de-input-search',
  miniSizex: 1,
  miniSizey: 1
}
const drawPanel = {
  type: 'custom',
  style: {
    width: 300,
    // height: 45.5,
    height: 90,
    fontSize: 14,
    fontWeight: 500,
    lineHeight: '',
    letterSpacing: 0,
    textAlign: '',
    color: ''
  },
  component: 'de-input-search'
}

class TextInputServiceImpl extends WidgetService {
  constructor(options = {}) {
    Object.assign(options, { name: 'textInputWidget' })
    super(options)
    this.filterDialog = true
    this.showSwitch = false
  }

  initLeftPanel() {
    const value = JSON.parse(JSON.stringify(leftPanel))
    return value
  }

  initFilterDialog() {
    const value = JSON.parse(JSON.stringify(dialogPanel))
    return value
  }

  initDrawPanel() {
    const value = JSON.parse(JSON.stringify(drawPanel))
    return value
  }

  filterFieldMethod(fields) {
    return fields.filter(field => {
      return field['deType'] === 0
    })
  }
  getParam(element, val) {
    let value = null
    if (!val) {
      value = this.fillValueDerfault(element)
    } else {
      value = Array.isArray(val) ? val : val.split(',')
    }
    const param = {
      component: element,
      value: !value ? [] : Array.isArray(value) ? value : [value],
      operator: 'like'
    }
    return param
  }
  fillValueDerfault(element) {
    const defaultV = element.options.value === null ? '' : element.options.value.toString()
    if (defaultV === null || typeof defaultV === 'undefined' || defaultV === '' || defaultV === '[object Object]') return null
    return defaultV.split(',')[0]
  }
}
const textInputServiceImpl = new TextInputServiceImpl()
export default textInputServiceImpl
