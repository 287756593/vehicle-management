import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import {
  Button,
  Cell,
  CellGroup,
  Field,
  Popup,
  Picker,
  Tabbar,
  TabbarItem,
  NavBar,
  Card,
  Tag,
  Icon,
  Image,
  Uploader,
  RadioGroup,
  Radio,
  Step,
  Steps,
  Dialog,
  Toast,
  Loading,
  Grid,
  GridItem,
  Empty
} from 'vant'
import 'vant/lib/index.css'
import './styles/global.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Button)
app.use(Cell)
app.use(CellGroup)
app.use(Field)
app.use(Popup)
app.use(Picker)
app.use(Tabbar)
app.use(TabbarItem)
app.use(NavBar)
app.use(Card)
app.use(Tag)
app.use(Icon)
app.use(Image)
app.use(Uploader)
app.use(RadioGroup)
app.use(Radio)
app.use(Step)
app.use(Steps)
app.use(Dialog)
app.use(Toast)
app.use(Loading)
app.use(Grid)
app.use(GridItem)
app.use(Empty)

app.mount('#app')
