import { useState } from 'react'
import './CreateCampaign.css'

const CreateCampaign = ({ onClose, onSubmit }) => {
    const [formData, setFormData] = useState({
        name: '',
        description: '',
        totalMilestones: ''
    })
    const [errors, setErrors] = useState({})
    const [submitting, setSubmitting] = useState(false)

    const handleChange = (e) => {
        const { name, value } = e.target
        setFormData(prev => ({ ...prev, [name]: value }))
        // Clear error when user starts typing
        if (errors[name]) {
            setErrors(prev => ({ ...prev, [name]: null }))
        }
    }

    const validate = () => {
        const newErrors = {}

        if (!formData.name.trim()) {
            newErrors.name = 'Название обязательно'
        }

        if (formData.totalMilestones && isNaN(formData.totalMilestones)) {
            newErrors.totalMilestones = 'Должно быть числом'
        }

        if (formData.totalMilestones && parseInt(formData.totalMilestones) < 1) {
            newErrors.totalMilestones = 'Должно быть больше 0'
        }

        return newErrors
    }

    const handleSubmit = async (e) => {
        e.preventDefault()

        const newErrors = validate()
        if (Object.keys(newErrors).length > 0) {
            setErrors(newErrors)
            return
        }

        setSubmitting(true)

        try {
            const submitData = {
                name: formData.name.trim(),
                description: formData.description.trim() || null,
                totalMilestones: formData.totalMilestones ? parseInt(formData.totalMilestones) : null
            }

            await onSubmit(submitData)
            onClose()
        } catch (error) {
            console.error('Error creating campaign:', error)
            setErrors({ submit: 'Не удалось создать кампанию' })
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <h2>📚 Создать кампанию</h2>
                    <button onClick={onClose} className="modal-close">×</button>
                </div>

                <form onSubmit={handleSubmit} className="campaign-form">
                    <div className="form-group">
                        <label htmlFor="name">Название кампании *</label>
                        <input
                            type="text"
                            id="name"
                            name="name"
                            value={formData.name}
                            onChange={handleChange}
                            placeholder="Проклятие Страда"
                            className={errors.name ? 'error' : ''}
                            maxLength={255}
                        />
                        {errors.name && <span className="error-message">{errors.name}</span>}
                    </div>

                    <div className="form-group">
                        <label htmlFor="description">Описание сюжета</label>
                        <textarea
                            id="description"
                            name="description"
                            value={formData.description}
                            onChange={handleChange}
                            placeholder="Классическая кампания по Равенлофту..."
                            rows={4}
                            maxLength={1000}
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="totalMilestones">
                            Количество вех (опционально)
                            <span className="label-hint">Только для мастера</span>
                        </label>
                        <input
                            type="number"
                            id="totalMilestones"
                            name="totalMilestones"
                            value={formData.totalMilestones}
                            onChange={handleChange}
                            placeholder="6"
                            min="1"
                            className={errors.totalMilestones ? 'error' : ''}
                        />
                        {errors.totalMilestones && (
                            <span className="error-message">{errors.totalMilestones}</span>
                        )}
                        <span className="field-hint">
                            Игроки будут видеть только процент прогресса
                        </span>
                    </div>

                    {errors.submit && (
                        <div className="submit-error">{errors.submit}</div>
                    )}

                    <div className="modal-actions">
                        <button
                            type="button"
                            onClick={onClose}
                            className="btn-secondary"
                            disabled={submitting}
                        >
                            Отмена
                        </button>
                        <button
                            type="submit"
                            className="btn-primary"
                            disabled={submitting}
                        >
                            {submitting ? 'Создание...' : 'Создать кампанию'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    )
}

export default CreateCampaign
