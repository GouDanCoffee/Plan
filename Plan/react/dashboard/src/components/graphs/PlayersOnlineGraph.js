import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import {tooltip} from "../../util/graphs";
import LineGraph from "./LineGraph";
import {ChartLoader} from "../navigation/Loader";

const PlayersOnlineGraph = ({data}) => {
    const {t} = useTranslation();
    const [series, setSeries] = useState([]);

    useEffect(() => {
        const playersOnlineSeries = {
            name: t('html.label.playersOnline'),
            type: 'areaspline',
            tooltip: tooltip.zeroDecimals,
            data: data.playersOnline,
            color: data.color,
            yAxis: 0
        }
        setSeries([playersOnlineSeries]);
    }, [data, t])

    if (!data) return <ChartLoader/>;

    return (
        <LineGraph id="players-online-graph" series={series}/>
    )
}

export default PlayersOnlineGraph