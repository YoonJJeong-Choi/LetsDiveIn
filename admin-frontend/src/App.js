import React, { useEffect } from 'react';
import { Provider, useDispatch } from 'react-redux';
import { BrowserRouter  } from 'react-router-dom'
import { ThemeSwitcherProvider } from 'react-css-theme-switcher';
import store from './store';
import history from './history'
import Layouts from './layouts'
import { THEME_CONFIG } from './configs/AppConfig';
import { checkAuth } from './store/slices/authSlice';
import './lang'

const themes = {
  dark: `${process.env.PUBLIC_URL}/css/dark-theme.css`,
  light: `${process.env.PUBLIC_URL}/css/light-theme.css`,
};

const AppContent = () => {
  const dispatch = useDispatch();
  
  useEffect(() => {
    // 앱 시작 시 세션 확인
    dispatch(checkAuth());
  }, [dispatch]);

  return <Layouts />;
};

function App() {
  return (
    <div className="App">
      <Provider store={store}>
        <BrowserRouter history={history}>
          <ThemeSwitcherProvider 
            themeMap={themes} 
            defaultTheme={THEME_CONFIG.currentTheme} 
            insertionPoint="styles-insertion-point"
          >
            <AppContent />
          </ThemeSwitcherProvider>
        </BrowserRouter>  
      </Provider>
    </div>
  );
}

export default App;
