import React from 'react'
import { Button } from "antd";
import { Link } from 'react-router-dom'
import Flex from 'components/shared-components/Flex'
import { useSelector } from 'react-redux'
import { ADMIN_LOGO_SRC } from 'configs/MediaConfig'

const ErrorTwo = () => {
	const theme = useSelector(state => state.theme.currentTheme)
	return (
		<div className={`h-100 ${theme === 'light' ? 'bg-white' : ''}`}>
			<div className="container-fluid d-flex flex-column justify-content-between h-100 px-md-4 pb-md-4 pt-md-1">
				<div>
					<img className="img-fluid" src="/img/LetsDiveIn03.png" alt="SWIM MALL logo" style={{ maxWidth: 160 }} />
				</div>
				<div className="container">
					<div className="text-center mb-5">
						<img className="img-fluid" src={ADMIN_LOGO_SRC} alt="" style={{ maxWidth: 280, opacity: 0.85 }} />
						<h1 className="font-weight-bold mb-4">Sorry, something goes wrong</h1>
						<Link to='/app'>
							<Button type="primary">Back to Home</Button>
						</Link>
					</div>
				</div>
				<Flex mobileFlex={false} justifyContent="center">
					<span>Copyright &copy; {`${new Date().getFullYear()}`} <span className="font-weight-semibold">Let's Dive In</span> All rights reserved.</span>
				</Flex>
			</div>
		</div>
	)
}

export default ErrorTwo

