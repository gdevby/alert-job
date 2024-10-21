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
				setStatistics(response.data)
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

	const dates = Object.entries(statistics)[0][1].map(({date}) => new Date(date));
	const data = Object.entries(statistics).map(([siteName, stats]) => {
		return { curve: "linear", label: siteName, data: stats.map(({numberOfOrders}) => numberOfOrders) };
	})

  return (
    <div style={{height: 384, marginTop: 32}}>
			<p style={{textAlign: 'center'}}>График количество заказов по дням</p>
			<LineChart
				yAxis={[{ label: 'количество заказов' }]}
				xAxis={[{ scaleType: 'utc', data: dates }]}
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
