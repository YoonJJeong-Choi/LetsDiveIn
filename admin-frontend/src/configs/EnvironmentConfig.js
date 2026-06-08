const dev = {
  API_ENDPOINT_URL: 'http://localhost:8080/api',
  CUSTOMER_WEB_URL: process.env.REACT_APP_CUSTOMER_WEB_URL || 'http://localhost:3000',
};

const prod = {
  API_ENDPOINT_URL: '/api',
  CUSTOMER_WEB_URL: process.env.REACT_APP_CUSTOMER_WEB_URL || '',
};

const test = {
  API_ENDPOINT_URL: '/api',
  CUSTOMER_WEB_URL: process.env.REACT_APP_CUSTOMER_WEB_URL || 'http://localhost:3000',
};

const getEnv = () => {
	switch (process.env.NODE_ENV) {
		case 'development':
			return dev
		case 'production':
			return prod
		case 'test':
			return test
		default:
			break;
	}
}

export const env = getEnv()
