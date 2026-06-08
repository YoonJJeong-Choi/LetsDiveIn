/** @jsxImportSource @emotion/react */
import AppBreadcrumb from 'components/layout-components/AppBreadcrumb';
import { css } from '@emotion/react';
import { MEDIA_QUERIES } from 'constants/ThemeConstant';

export const PageHeader = ({ title, display }) => {
	return (
		display ? (
			<div
				css={css`
					align-items: center;
					margin-bottom: 1rem;

					@media ${MEDIA_QUERIES.LAPTOP_ABOVE} {
						display: flex;
					}
				`}
			>
				<AppBreadcrumb fallbackTitle={title} />
			</div>
		)
		: null
	)
}

export default PageHeader