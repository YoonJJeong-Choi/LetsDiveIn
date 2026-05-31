import React from 'react'
import LoginForm from '../../components/LoginForm'
import { Card, Row, Col } from "antd";

const backgroundStyle = {
	backgroundColor: '#eaf7ff'
}

const LoginOne = props => {
	return (
		<div className="h-100" style={backgroundStyle}>
			<div className="container d-flex flex-column justify-content-center h-100">
				<Row justify="center">
					<Col xs={20} sm={20} md={20} lg={7}>
						<Card>
							<div className="my-4">
								<div className="text-center">
									<img
										className="img-fluid mb-4"
										src="/img/LetsDiveIn03.png"
										alt="SWIM MALL logo"
										style={{ maxWidth: 180, maxHeight: 80, objectFit: 'contain' }}
									/>
									<h4 className="font-weight-bold mb-4">{"운영센터"}</h4>
								</div>
								<Row justify="center">
									<Col xs={24} sm={24} md={20} lg={20}>
										<LoginForm {...props} />
									</Col>
								</Row>
							</div>
						</Card>
					</Col>
				</Row>
			</div>
		</div>
	)
}

export default LoginOne
