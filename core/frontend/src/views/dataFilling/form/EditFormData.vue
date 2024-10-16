<script>
import {cloneDeep, concat, filter, find, floor, forEach, includes, keys, map, parseInt, split} from 'lodash-es'
import {EMAIL_REGEX, PHONE_REGEX} from '@/utils/validate'
import {
  getTableColumnData,
  newFormRowData,
  saveFormRowData,
  userFillFormData
} from '@/views/dataFilling/form/dataFilling'
import GridTable from '@/components/gridTable/index.vue'
import {deepCopy} from "@/components/canvas/utils/utils";

export default {
  name: 'EditFormData',
  components: { GridTable },
  props: {
    keyName: {
      type: String,
      required: false
    },
    id: {
      type: String,
      required: false
    },
    userTaskId: {
      type: String,
      required: false
    },
    title: {
      type: String,
      required: false
    },
    formTitle: {
      type: String,
      required: true
    },
    formId: {
      type: String,
      required: true
    },
    forms: {
      type: Object,
      required: true
    },
    data: {
      type: Object,
      required: false
    },
    dataList: {
      type: Object,
      required: false
    },
    showDrawer: {
      type: Boolean,
      required: true
    },
    readonly: {
      type: Boolean,
      required: true
    }
  },
  data() {
    const checkDateRangeRequireValidator = (rule, value, callback) => {
      if (!value) {
        return callback(new Error(this.$t('commons.required')))
      }
      if (value.length < 2) {
        return callback(new Error(this.$t('commons.required')))
      }
      if (!value[0]) {
        return callback(new Error(this.$t('commons.required')))
      }
      if (!value[1]) {
        return callback(new Error(this.$t('commons.required')))
      }
      callback()
    }
    return {
      currentPage: 1,
      loading: false,
      asyncOptions: {},
      drawer: false,
      modifyRows: false,
      modifyRowsIndex: "",
      columns: [],
      tempRows: [],
      formData: [],
      formDataList: [],
      formDataList4Excel: [],
      _tempForms: [],
      requiredRule: { required: true, message: this.$t('commons.required'), trigger: ['blur', 'change'] },
      dateRangeRequiredRule: { validator: checkDateRangeRequireValidator, message: this.$t('commons.required'), trigger: ['blur', 'change'] },
      inputTypes: [
        { type: 'text', name: this.$t('data_fill.form.text'), rules: [] },
        { type: 'number', name: this.$t('data_fill.form.number'), rules: [] },
        {
          type: 'tel',
          name: this.$t('data_fill.form.tel'),
          rules: [{ pattern: PHONE_REGEX, message: this.$t('user.mobile_number_format_is_incorrect'), trigger: ['blur', 'change'] }]
        },
        {
          type: 'email',
          name: this.$t('data_fill.form.email'),
          rules: [{ pattern: EMAIL_REGEX, message: this.$t('user.email_format_is_incorrect'), trigger: ['blur', 'change'] }]
        }
      ],
      pickerOptions: {
        disabledDate: (time) => {
          return time.getTime() < new Date(0).getTime()
        }
      }
    }
  },
  computed: {
    allData() {
      if (this.dataList && this.dataList.length > 0) {
        return this.dataList
      }
      if (this.data) {
        return [this.data]
      }
      return [{}]
    }
  },
  watch: {},
  mounted() {
    this._tempForms = []
    this.tempRows = []
    this.formData = []
    this.asyncOptions = {}
    this.currentPage = 1
    forEach(this.allData, _data => {
      const _tempFormRow = []
      forEach(this.forms, v => {
        if (!v.removed) {
          const f = cloneDeep(v)
          if (f.type === 'date' && f.settings.dateType === undefined) { // 兼容旧的
            f.settings.dateType = f.settings.enableTime ? 'datetime' : 'date'
          }
          if (f.type === 'dateRange' && f.settings.dateType === undefined) { // 兼容旧的
            f.settings.dateType = f.settings.enableTime ? 'datetimerange' : 'daterange'
          }
          if (f.type === 'dateRange') {
            const _start = _data[f.settings.mapping.columnName1]
            const _end = _data[f.settings.mapping.columnName2]
            f.value = [_start, _end]
          } else {
            // 交给后面处理
            f.value = _data[f.settings.mapping.columnName]
          }
          _tempFormRow.push(f)
        }
      })
      this._tempForms.push(_tempFormRow)
    })

    this.loading = true
    this.initFormOptionsData(this._tempForms, () => {
      // 最后处理选项值
      for (let i = 0; i < this._tempForms.length; i++) {
        const row = this._tempForms[i]
        row.forEach(f => {
          if (f.type !== 'dateRange') {
            const _value = this.allData[i][f.settings.mapping.columnName]
            if (f.type === 'select' && f.settings.multiple || f.type === 'checkbox') {
              if (_value) {
              // 过滤一下选项值
                if (this.readonly) {
                  f.value = JSON.parse(_value)
                } else {
                  const tempId = f.settings.optionDatasource + '_' + f.settings.optionTable + '_' + f.settings.optionColumnKey + '_' + f.settings.optionColumnValue + '_' + f.settings.optionOrder
                  const options = map(f.settings.optionSourceType === 1 ? f.settings.options : (this.asyncOptions[tempId] ? this.asyncOptions[tempId] : []), f => f.value)
                  f.value = filter(JSON.parse(_value), v => includes(options, v))
                }
              } else {
                f.value = []
              }
            } else if (f.type === 'select' && !f.settings.multiple || f.type === 'radio') {
              if (_value) {
                if (!this.readonly) {
                  const tempId = f.settings.optionDatasource + '_' + f.settings.optionTable + '_' + f.settings.optionColumnKey + '_' + f.settings.optionColumnValue + '_' + f.settings.optionOrder
                  const options = map(f.settings.optionSourceType === 1 ? f.settings.options : (this.asyncOptions[tempId] ? this.asyncOptions[tempId] : []), f => f.value)
                  if (!includes(options, _value)) {
                    f.value = undefined
                  } else {
                    f.value = _value
                  }
                } else {
                  f.value = _value
                }
              }
            }
          }
          f.tempId = f.settings ? f.settings.optionDatasource + '_' + f.settings.optionTable + '_' + f.settings.optionColumnKey + '_' + f.settings.optionColumnValue + '_' + f.settings.optionOrder : 'unset'
        })
      }
      // 赋值到表单

      this.formData = cloneDeep(this._tempForms);
      this.loading = false
    })

      forEach(filter(this.forms, f => !f.removed), f => {
        if (f.type === 'dateRange') {
          this.columns.push({
            props: f.settings?.mapping?.columnName1,
            label: f.settings?.name,
            date: true,
            dateType: f.settings?.dateType ? f.settings?.dateType : (f.settings?.enableTime ? 'datetimerange' : 'daterange'),
            type: f.type,
            multiple: !!f.settings.multiple,
            rangeIndex: 0
          })
          this.columns.push({
            props: f.settings?.mapping?.columnName2,
            label: f.settings?.name,
            date: true,
            dateType: f.settings?.dateType ? f.settings?.dateType : (f.settings?.enableTime ? 'datetimerange' : 'daterange'),
            type: f.type,
            multiple: !!f.settings.multiple,
            rangeIndex: 1
          })
        } else {
          this.columns.push({
            props: f.settings?.mapping?.columnName,
            label: f.settings?.name,
            date: f.type === 'date',
            dateType: f.type === 'date' ? (f.settings?.dateType ? f.settings?.dateType : (f.settings?.enableTime ? 'datetime' : 'date')) : undefined,
            type: f.type,
            multiple: !!f.settings.multiple
          })
        }
      })

     map(filter(this.columns, c => c.date), 'props')

    console.log(this.columns);
  },
  methods: {

    handleClose(done) {

    },

    initFormOptionsData(forms, callback) {
      const queries = []
      const queryIds = []
      // 同一个表单多条数据，展示的肯定也是相同的，所以取第一个
      forEach(forms[0], f => {
        if (f.type === 'checkbox' || f.type === 'select' || f.type === 'radio') {
          if (f.settings && f.settings.optionSourceType === 2 && f.settings.optionDatasource && f.settings.optionTable && f.settings.optionColumnKey && f.settings.optionColumnValue && f.settings.optionOrder) {
            const id = f.settings.optionDatasource + '_' + f.settings.optionTable + '_' + f.settings.optionColumnKey + '_' + f.settings.optionColumnValue + '_' + f.settings.optionOrder

            const p = getTableColumnData(f.settings.optionDatasource, f.settings.optionTable, f.settings.optionColumnKey , f.settings.optionColumnValue  , f.settings.optionOrder)
            queries.push(p)
            queryIds.push(id)
          }
        }
      })

      if (queries.length > 0) {
        Promise.all(queries).then((val) => {
          for (let i = 0; i < queryIds.length; i++) {
            const id = queryIds[i]
            this.asyncOptions[id] = val[i].data
          }
        }).finally(() => {
          if (callback) {
            callback()
          }
        })
      } else {
        if (callback) {
          callback()
        }
      }
    },
    getRules(item) {
      let rules = []
      if (item.settings.required) {
        rules.push(this.requiredRule)
        if (item.type === 'dateRange') {
          rules.push(this.dateRangeRequiredRule)
        }
      }
      if (item.type === 'input') {
        const inputRules = find(this.inputTypes, t => t.type === item.settings.inputType).rules
        if (inputRules) {
          rules = concat(rules, inputRules)
        }
      }
      return rules
    },
    closeDrawer() {
      this.$emit('update:showDrawer', false)
    },
    onNumberChange(item) {
      let value
      if (item.settings.mapping.type === 'number') {
        value = floor(item.value, 0)
      } else {
        value = floor(item.value, 8)
      }
      this.$nextTick(() => {
        item.value = value
      })
    },
    onPageChange(page) {
      this.currentPage = page
    },

    formatDate(value, dateType) {
      if (!value) {
        return value
      }
      const value_date = new Date(value);
      switch (dateType) {
        case 'year':
          return value_date.format('yyyy')
        case 'month':
        case 'monthrange':
          return value_date.format('yyyy-MM')
        case 'datetime':
        case 'datetimerange':
          return value_date.format('yyyy-MM-dd hh:mm:ss')
        default:
          return value_date.format('yyyy-MM-dd')
      }
    },

    openDetails() {
      this.drawer = true;
    },

    confirmModify() {

      this.$refs['mForm'].validate((valid, invalidFields) => {
        if (valid) {
          this.formDataList.splice(this.modifyRowsIndex , 1 ,this.formData);
          this.dealFormDataList();
          this.formData = cloneDeep(this._tempForms);
          this.modifyRows = false;
        } else {
          // 获取第几页，切换到对应的页面
          const _key = keys(invalidFields)[0]
          const index = split(_key, ']')[0].replace('[', '')
          this.currentPage = parseInt(index) + 1
          this.loading = false
        }
      })

    },

    cancelModify () {
      //取消修改，清空现有数据
      this.modifyRows = false;
      this.formData = cloneDeep(this._tempForms);
    },


    addAnother(){
      this.$refs['mForm'].validate((valid, invalidFields) => {
        if (valid) {
          this.formDataList.push(cloneDeep(this.formData));
          this.formData = cloneDeep(this._tempForms);
          this.dealFormDataList();
        } else {
          // 获取第几页，切换到对应的页面
          const _key = keys(invalidFields)[0]
          const index = split(_key, ']')[0].replace('[', '')
          this.currentPage = parseInt(index) + 1
          this.loading = false
        }
      })
    },

    dealFormData (formData , index) {
      const _data = {}
      _data.index = index;
      for (let i = 0; i < formData.length; i++) {
        const row = formData[i]
        forEach(row, f => {
          if (f.type === 'dateRange') {
            const _start = f.settings.mapping.columnName1
            const _end = f.settings.mapping.columnName2
            if (f.value) {
              if (f.value[0]) {
                _data[_start] = f.value[0].getTime()
              }
              if (f.value[1]) {
                _data[_end] = f.value[1].getTime()
              }
            }
          } else {
            const name = f.settings.mapping.columnName
            if (f.type === 'select' && f.settings.multiple || f.type === 'checkbox') {
              if (f.value) {
                _data[name] = JSON.stringify(f.value)
              }
            } else if (f.type === 'date' && f.value) {
              console.log("date f.value: " + f.value);
              _data[name] = f.value.getTime()
            } else {
              _data[name] = f.value
            }
          }
        })
      }
      return _data;
    },

    dealFormDataList () {
       this.formDataList4Excel = [];
       let index = 0;
       forEach(this.formDataList , f => {
         this.formDataList4Excel.push(this.dealFormData(f , index));
         index++ ;
      })
    },

    updateRow(index){
      this.modifyRowsIndex = index,
      this.formData = this.formDataList[index];
      this.drawer = false;
      this.modifyRows = true;
    },

    deleteRow(index){
      this.formDataList4Excel.splice(index , 1);
      this.formDataList.splice(index , 1);
    },

    needInsertNewData() {
      let needInsert = false;
      for (let i = 0; i < this.formData.length; i++) {
        const row = this.formData[i]
        forEach(row, f => {
          if (f.type === 'dateRange') {
            if (f.value) {
              if (f.value[0]) {
                needInsert = true
              }
              if (f.value[1]) {
                needInsert = true;
              }
            }
          } else {
            if (f.type === 'select' && f.settings.multiple || f.type === 'checkbox') {
              if (f.value) {
                needInsert = true;
              }
            } else if (f.type === 'date' && f.value) {
              needInsert = true;
            } else if ( f.value) {
              needInsert = true;
            }
          }
        })
      }
      return needInsert;
    },

    save() {
      if (this.needInsertNewData()) {
        this.formDataList.push(cloneDeep(this.formData));
      }
      const req = []
      forEach(this.formDataList , formData => {
        for (let i = 0; i < formData.length; i++) {
          const row = formData[i]
          const _data = {}
          forEach(row, f => {
            if (f.type === 'dateRange') {
              const _start = f.settings.mapping.columnName1
              const _end = f.settings.mapping.columnName2
              if (f.value) {
                if (f.value[0]) {
                  _data[_start] = f.value[0].getTime()
                }
                if (f.value[1]) {
                  _data[_end] = f.value[1].getTime()
                }
              }
            } else {
              const name = f.settings.mapping.columnName
              if (f.type === 'select' && f.settings.multiple || f.type === 'checkbox') {
                if (f.value) {
                  _data[name] = JSON.stringify(f.value)
                }
              } else if (f.type === 'date' && f.value) {
                _data[name] = f.value.getTime()
              } else {
                _data[name] = f.value
              }
            }
          })
          if (this.keyName) {
            _data[this.keyName] = this.allData[i][this.keyName]
          }
          req.push(_data)
        }
      })

      if (this.userTaskId) {
        userFillFormData(this.userTaskId, req).then(res => {
          this.$emit('save-success')
        }).finally(() => {
          this.loading = false
        })
      } else {
        // 非任务都是针对单条数据进行提交
        if (this.id !== undefined) {
          // update
          saveFormRowData(this.formId, this.id, req[0]).then(res => {
            this.$emit('save-success')
          }).finally(() => {
            this.loading = false
          })
        } else {
          // insert
          newFormRowData(this.formId, req).then(res => {
            this.$emit('save-success')
          }).finally(() => {
            this.loading = false
          })
        }
      }
    },

    doSave() {
      this.loading = true
      //不需要新增，即所有数据均为空，直接保存。
      if (!this.needInsertNewData()) {
        this.save();
      } else {
        this.$refs['mForm'].validate((valid, invalidFields) => {
          if (valid) {
            this.save();
          } else {
            // 获取第几页，切换到对应的页面
            const _key = keys(invalidFields)[0]
            const index = split(_key, ']')[0].replace('[', '')
            this.currentPage = parseInt(index) + 1

            this.loading = false
          }
        })
      }
    }
  }

}
</script>

<template>
  <el-container
    v-loading="loading"
    class="DataFillingSave"
  >
    <el-header class="de-header">
      <div class="panel-info-area">
        <span class="text16 margin-left12">
          {{ title? title: (readonly? $t('data_fill.task.show_data'): $t('data_fill.task.edit_data')) }}
        </span>
      </div>

      <div style="padding-right: 20px">
        <i
          class="el-icon-close"
          style="cursor: pointer"
          @click="closeDrawer"
        />
      </div>
    </el-header>
    <el-main class="de-main">
      <div style="width: 80% ; height: 100%">
        <div class="m-title">{{ formTitle }}</div>

      <div style="width: 100%; overflow: auto ; " :style="{height: !readonly && !id ? '55%' : '100%'}">
      <el-form
        ref="mForm"
        class="m-form"
        style="width: 100%"
        label-position="top"
        hide-required-asterisk
        :model="formData"
        @submit.native.prevent
      >
        <div
          v-for="(row, $index1) in formData"
          v-show="currentPage === $index1 + 1"
          :key="$index1"
        >
          <div
            v-for="(item, $index2) in row"
            :key="item.id"
            style="float: left"
            :style="{width: item.settings.width + '%'}"
            class="m-item m-form-item"
          >

            <div class="m-label-container" >
              <span style="width: unset">
                {{ item.settings.name }}
                <span
                  v-if="item.settings.required"
                  style="color: red"
                >*</span>
              </span>
            </div>
            <el-form-item
              :prop="'['+ $index1 +']['+ $index2 +'].value'"
              class="form-item"
              style="margin-bottom: 5px"
              :readonly="readonly"
              :rules="getRules(item)"
            >
              <el-input
                v-if="item.type === 'input' && item.settings.inputType !== 'number'"
                v-model="item.value"
                :type="item.settings.inputType"
                :required="item.settings.required"
                :readonly="readonly"
                :placeholder="item.settings.placeholder"
                size="small"
                :show-word-limit="item.value !== undefined && item.value !== null && item.value.length > 250"
                maxlength="255"
              />
              <el-input-number
                v-if="item.type === 'input' && item.settings.inputType === 'number'"
                v-model="item.value"
                :required="item.settings.required"
                :disabled="readonly"
                :placeholder="item.settings.placeholder"
                style="width: 100%"
                controls-position="right"
                :precision="item.settings.mapping.type === 'number' ? 0 : undefined"
                size="small"
                :min="-999999999999"
                :max="999999999999"
                @change="onNumberChange(item)"
                @blur="onNumberChange(item)"
                @keyup.enter.native="onNumberChange(item)"
              />
              <el-input
                v-else-if="item.type === 'textarea'"
                v-model="item.value"
                type="textarea"
                :required="item.settings.required"
                :readonly="readonly"
                :placeholder="item.settings.placeholder"
                size="small"
              />
              <el-select
                v-else-if="item.type === 'select'"
                v-model="item.value"
                :required="item.settings.required"
                :disabled="readonly"
                :placeholder="item.settings.placeholder"
                style="width: 100%"
                size="small"
                :multiple="item.settings.multiple"
                clearable
              >
                <el-option
                  v-for="(x, $index) in item.settings.optionSourceType === 1 ? item.settings.options : (asyncOptions[item.tempId] ? asyncOptions[item.tempId] : [])"
                  :key="$index"
                  :label="x.name"
                  :value="x.value"
                />
              </el-select>
              <el-radio-group
                v-else-if="item.type === 'radio'"
                v-model="item.value"
                :required="item.settings.required"
                :disabled="readonly"
                style="width: 100%"
                size="small"
              >
                <el-radio
                  v-for="(x, $index) in item.settings.optionSourceType === 1 ? item.settings.options : (asyncOptions[item.tempId] ? asyncOptions[item.tempId] : [])"
                  :key="$index"
                  :label="x.value"
                >{{ x.name }}
                </el-radio>
              </el-radio-group>
              <el-checkbox-group
                v-else-if="item.type === 'checkbox'"
                v-model="item.value"
                :required="item.settings.required"
                :disabled="readonly"
                size="small"
              >
                <el-checkbox
                  v-for="(x, $index) in item.settings.optionSourceType === 1 ? item.settings.options : (asyncOptions[item.tempId] ? asyncOptions[item.tempId] : [])"
                  :key="$index"
                  :label="x.value"
                >{{ x.name }}
                </el-checkbox>
              </el-checkbox-group>
              <el-date-picker
                v-else-if="item.type === 'date'"
                v-model="item.value"
                :required="item.settings.required"
                :readonly="readonly"
                :type="item.settings.dateType"
                :placeholder="item.settings.placeholder"
                style="width: 100%"
                size="small"
                :picker-options="pickerOptions"
              />
              <el-date-picker
                v-else-if="item.type === 'dateRange'"
                v-model="item.value"
                :required="item.settings.required"
                :readonly="readonly"
                :type="item.settings.dateType"
                :range-separator="item.settings.rangeSeparator"
                :start-placeholder="item.settings.startPlaceholder"
                :end-placeholder="item.settings.endPlaceholder"
                style="width: 100%"
                size="small"
                :picker-options="pickerOptions"
              />
            </el-form-item>
          </div>
        </div>
      </el-form>
      </div>
      <div style="width: 100%; height: 30%; margin-top: 40px" v-if="!readonly && !id ">
          <div class="m-title">已添加明细</div>
          <grid-table
            v-if="columns.length > 0"
            ref="dataTable"
            style="width: 100%; height: 100%; padding: 8px 20px"
            stripe
            :table-data="formDataList4Excel"
            :show-pagination = false
            :columns="[]"
          >
            <el-table-column
              v-for="c in columns"
              :key="c.props"
              :prop="c.props"
            >
              <template
                slot="header"
              >
                {{ c.label }}
                <span v-if="c.rangeIndex === 0">({{ $t('data_fill.data.start') }})</span>
                <span v-if="c.rangeIndex === 1">({{ $t('data_fill.data.end') }})</span>
              </template>
              <template slot-scope="scope">
                <span
                  v-if="c.date && scope.row[c.props]"
                  style="white-space:nowrap; width: fit-content"
                  :title="formatDate(scope.row[c.props], c.dateType)"
                >
                  {{ formatDate(scope.row[c.props], c.dateType) }}
                </span>
                <template v-else-if="(c.type === 'select' && c.multiple || c.type === 'checkbox') && scope.row[c.props]">
                  <div
                    v-for="(x, $index) in JSON.parse(scope.row[c.props])"
                    :key="$index"
                    style="white-space:nowrap; width: fit-content"
                    :title="x"
                  >
                    {{ x }}
                  </div>
                </template>
                <span
                  v-else
                  style="white-space:nowrap; width: fit-content"
                  :title="scope.row[c.props]"
                >
                  {{ scope.row[c.props] }}
                </span>
              </template>
            </el-table-column>
            <el-table-column
              :label="$t('data_fill.form.operation')"
              width="160"
              fixed="right"
            >
              <template slot-scope="scope">
                <el-button
                  type="text"
                  @click="updateRow(scope.row.index)"
                >
                  {{ $t('data_fill.form.modify') }}
                </el-button>
                <el-button
                  type="text"
                  @click="deleteRow(scope.row.index)"
                >
                  {{ $t('data_fill.form.delete') }}
                </el-button>
              </template>
            </el-table-column>
          </grid-table>
        </div>

<!--        <el-drawer-->
<!--          title="已添加明细"-->
<!--          direction="btt"-->
<!--          :append-to-body="true"-->
<!--          size="90%"-->
<!--          :visible.sync="drawer">-->
<!--          <el-container-->
<!--            direction="vertical"-->
<!--            style="display: flex; flex-direction: column ; width: 90%; margin-left: 5%"-->
<!--          >-->
<!--            <template>-->
<!--              <div style="flex: 1">-->
<!--                <grid-table-->
<!--                  v-if="columns.length > 0"-->
<!--                  ref="dataTable"-->
<!--                  style="width: 100%; height: 100%"-->
<!--                  border-->
<!--                  stripe-->
<!--                  :table-data="formDataList4Excel"-->
<!--                  :show-pagination = false-->
<!--                  :columns="[]"-->
<!--                >-->
<!--                  <el-table-column-->
<!--                    v-for="c in columns"-->
<!--                    :key="c.props"-->
<!--                    :prop="c.props"-->
<!--                  >-->
<!--                    <template-->
<!--                      slot="header"-->
<!--                    >-->
<!--                      {{ c.label }}-->
<!--                      <span v-if="c.rangeIndex === 0">({{ $t('data_fill.data.start') }})</span>-->
<!--                      <span v-if="c.rangeIndex === 1">({{ $t('data_fill.data.end') }})</span>-->
<!--                    </template>-->
<!--                    <template slot-scope="scope">-->
<!--                <span-->
<!--                  v-if="c.date && scope.row[c.props]"-->
<!--                  style="white-space:nowrap; width: fit-content"-->
<!--                  :title="formatDate(scope.row[c.props], c.dateType)"-->
<!--                >-->
<!--                  {{ formatDate(scope.row[c.props], c.dateType) }}-->
<!--                </span>-->
<!--                      <template v-else-if="(c.type === 'select' && c.multiple || c.type === 'checkbox') && scope.row[c.props]">-->
<!--                        <div-->
<!--                          v-for="(x, $index) in JSON.parse(scope.row[c.props])"-->
<!--                          :key="$index"-->
<!--                          style="white-space:nowrap; width: fit-content"-->
<!--                          :title="x"-->
<!--                        >-->
<!--                          {{ x }}-->
<!--                        </div>-->
<!--                      </template>-->
<!--                      <span-->
<!--                        v-else-->
<!--                        style="white-space:nowrap; width: fit-content"-->
<!--                        :title="scope.row[c.props]"-->
<!--                      >-->
<!--                  {{ scope.row[c.props] }}-->
<!--                </span>-->
<!--                    </template>-->
<!--                  </el-table-column>-->
<!--                  <el-table-column-->
<!--                    :label="$t('data_fill.form.operation')"-->
<!--                    width="160"-->
<!--                    fixed="right"-->
<!--                  >-->
<!--                    <template slot-scope="scope">-->
<!--                      <el-button-->
<!--                        type="text"-->
<!--                        @click="updateRow(scope.row.index)"-->
<!--                      >-->
<!--                        {{ $t('data_fill.form.modify') }}-->
<!--                      </el-button>-->
<!--                      <el-button-->
<!--                        type="text"-->
<!--                        @click="deleteRow(scope.row.index)"-->
<!--                      >-->
<!--                        {{ $t('data_fill.form.delete') }}-->
<!--                      </el-button>-->
<!--                    </template>-->
<!--                  </el-table-column>-->
<!--                </grid-table>-->
<!--              </div>-->
<!--            </template>-->
<!--          </el-container>-->
<!--        </el-drawer>-->


      </div>

    </el-main>
    <el-footer
      class="de-footer"
    >
      <div class="de-footer-container">
        <el-pagination
          v-if="allData.length > 1"
          ref="mPagerRef"
          layout="prev, pager, next"
          page-size="1"
          :total="allData.length"
          :current-page="currentPage"
          @current-change="onPageChange"
        />
      </div>
<!--      <el-button @click="closeDrawer">{{ $t("commons.cancel") }}</el-button>-->

      <el-button
        v-if="!readonly && !id && modifyRows"
        type="primary"
        @click="cancelModify"
      >取消修改
      </el-button>

      <el-button
        v-if="!readonly && !id && modifyRows"
        type="primary"
        @click="confirmModify"
      >确认修改
      </el-button>


      <el-button
        v-if="!readonly && !id "
        :disabled="modifyRows"
        type="primary"
        @click="addAnother"
      >{{ $t("commons.continue_add") }}
      </el-button>

<!--      <el-button-->
<!--        v-if="!readonly && !id "-->
<!--        type="primary"-->
<!--        :disabled="modifyRows"-->
<!--        @click="openDetails"-->
<!--      >查看 {{formDataList.length}} 条明细-->
<!--      </el-button>-->

      <el-button
        v-if="!readonly"
        type="primary"
        :disabled="modifyRows"
        @click="doSave"
      >{{ $t("commons.submit") }}
      </el-button>
    </el-footer>
  </el-container>
</template>

<style  lang="scss" scoped>
.DataFillingSave {

  height: 100%;

  .de-header {
    height: 56px !important;
    padding: 0px !important;
    border-bottom: 1px solid #E6E6E6;
    background-color: var(--SiderBG, white);

    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: space-between
  }

  .de-footer {
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: flex-end;

    .de-footer-container{
      flex: 1;
      display: flex;
      flex-direction: row;
      align-items: center;
      justify-content: center;
    }
  }

  .panel-info-area {
    padding-left: 20px;
  }

  .de-main {
    display: flex;
    align-items: center;
    flex-direction: column;

    .m-form {
      width: 80%;
    }
  }

  .m-title {
    margin: 15px 20px 10px;

    height: 28px;

    font-weight: 500;
    font-size: 20px;
    line-height: 28px;

    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .m-item {
    width: 100%;
    border: solid 1px #eee;
    background-color: #f1f1f1;
    border-radius: 4px;
  }

  .m-form-item {
    margin-bottom: 5px;
    border-radius: 4px;

    border: solid 1px transparent;
    background-color: unset;

    padding: 2px 20px;
  }

  .m-label-container {
    width: 100%;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;

    font-weight: 500;
    font-size: 14px;
    line-height: 22px;

    margin-bottom: 8px;

  }
}
</style>
