import React from "react";
import WorldPieCard from "../../common/WorldPieCard";
import {useParams} from "react-router-dom";
import {useDataRequest} from "../../../../hooks/dataFetchHook";
import {fetchWorldPie} from "../../../../service/serverService";
import {ErrorViewBody} from "../../../../views/ErrorView";
import {CardLoader} from "../../../navigation/Loader";

const ServerWorldPieCard = () => {
    const {identifier} = useParams();

    const {data, loadingError} = useDataRequest(fetchWorldPie, [identifier]);

    if (loadingError) return <ErrorViewBody error={loadingError}/>
    if (!data) return <CardLoader/>;

    return (
        <WorldPieCard
            worldSeries={data.world_series}
            gmSeries={data.gm_series}
        />
    )
}

export default ServerWorldPieCard;