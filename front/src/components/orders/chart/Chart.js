import React, { useState, useEffect } from 'react';
import { LineChart } from '@mui/x-charts/LineChart/LineChart';
import { parserService } from "../../../services/parser/endponits/parserService";

const Chart = () => {
  const [statistics, setStatistics] = useState();
  const [isLoading, setIsLoading] = useState(false);

	useEffect(() => {
    setIsLoading(true);

		parserService.getStatistics()
			.then(response => {
				const contentType = response.headers['content-type'];

				if (contentType !== 'application/json') {
					setStatistics(undefined)
					return;
				}

				const haveData = Object.keys(response.data).length > 0;
				setStatistics(haveData ? response.data : undefined)
			})
			.catch(() => {
        setStatistics(undefined);
			})
			.finally(() => {
				setIsLoading(false)
			})
	}, [])

	if (isLoading || !statistics) {
		return null;
	}

	const dates = Object.entries(statistics).reduce((acc, [_, data]) => {
		const result = [...acc, ...data.map(({ date }) => date)];
		return result;
	}, []);
	const uniqueDates = [...new Set(dates)].sort((a, b) => new Date(a) - new Date(b));

	const data = Object.entries(statistics).map(([siteName, stats]) => {
		const statsByDate = stats.reduce((acc, { date, numberOfOrders }) => {
			acc[date] = numberOfOrders;
			return acc;
		}, {});
		return { curve: 'linear', label: siteName, data: uniqueDates.map(date => statsByDate[date] ?? 0) };
	});

  return (
    <div style={{height: 384, marginTop: 32}}>
			<p style={{textAlign: 'center'}}>График количество заказов по дням</p>
			<LineChart
				yAxis={[{ label: 'количество заказов' }]}
				xAxis={[{ scaleType: 'utc', data: uniqueDates.map(date => new Date(date)) }]}
				series={data}
				slotProps={{ legend: {
					position: {
						horizontal: 'middle',
						vertical: 'bottom'
					},
					padding: 0,
					labelStyle: { fontSize: 12, textTransform: 'lowercase' }
				}}}
			/>
    </div>
  )
}

export default Chart
