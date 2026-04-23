import { combineReducers } from 'redux'
import theme from './slices/themeSlice'
import auth from './slices/authSlice'
import partner from './slices/partnerSlice'
import admin from './slices/adminSlice'
import inventory from './slices/inventorySlice'

const rootReducer = (asyncReducers) => (state, action) => {
    const combinedReducer = combineReducers({
        theme,
        auth,
        partner,
        admin,
        inventory,
        ...asyncReducers,
    })
    return combinedReducer(state, action)
}
  
export default rootReducer
