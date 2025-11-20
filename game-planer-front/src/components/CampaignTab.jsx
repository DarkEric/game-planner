import { useState } from 'react'
import CampaignList from './CampaignList'
import CreateCampaign from './CreateCampaign'
import { campaignApi } from '../services/campaignApi'
import './CampaignTab.css'

const CampaignTab = () => {
    const [showCreateModal, setShowCreateModal] = useState(false)
    const [selectedCampaignId, setSelectedCampaignId] = useState(null)
    const [refreshKey, setRefreshKey] = useState(0)

    const handleCreateCampaign = async (campaignData) => {
        try {
            await campaignApi.createCampaign(campaignData)
            setRefreshKey(prev => prev + 1) // Trigger refresh
            setShowCreateModal(false)
        } catch (error) {
            console.error('Error creating campaign:', error)
            throw error
        }
    }

    const handleSelectCampaign = (campaignId) => {
        setSelectedCampaignId(campaignId)
        // TODO: Navigate to campaign details
        console.log('Selected campaign:', campaignId)
    }

    return (
        <div className="campaign-tab">
            <CampaignList
                key={refreshKey}
                onSelectCampaign={handleSelectCampaign}
                onCreateCampaign={() => setShowCreateModal(true)}
            />

            {showCreateModal && (
                <CreateCampaign
                    onClose={() => setShowCreateModal(false)}
                    onSubmit={handleCreateCampaign}
                />
            )}
        </div>
    )
}

export default CampaignTab
