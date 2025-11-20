import { useState } from 'react'
import CampaignList from './CampaignList'
import CampaignDetails from './CampaignDetails'
import CreateCampaign from './CreateCampaign'
import { campaignApi } from '../services/campaignApi'
import './CampaignTab.css'

const CampaignTab = ({ currentUserId }) => {
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
    }

    const handleBackToList = () => {
        setSelectedCampaignId(null)
        setRefreshKey(prev => prev + 1) // Refresh list when going back
    }

    return (
        <div className="campaign-tab">
            {selectedCampaignId ? (
                <CampaignDetails
                    campaignId={selectedCampaignId}
                    currentUserId={currentUserId}
                    onBack={handleBackToList}
                />
            ) : (
                <CampaignList
                    key={refreshKey}
                    onSelectCampaign={handleSelectCampaign}
                    onCreateCampaign={() => setShowCreateModal(true)}
                />
            )}

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
