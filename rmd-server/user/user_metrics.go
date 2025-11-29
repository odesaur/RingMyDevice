package user

import "rmd-server/metrics"

func initializeUserMetrics(db *RMDDB) {
	var userCount int64
	db.DB.Model(&RMDUser{}).Count(&userCount)
	metrics.Accounts.Set(float64(userCount))

	var locationCount int64
	db.DB.Model(&Location{}).Count(&locationCount)
	metrics.Locations.Set(float64(locationCount))

	var pictureCount int64
	db.DB.Model(&Picture{}).Count(&pictureCount)
	metrics.Pictures.Set(float64(pictureCount))

	var pendingCommandCount int64
	db.DB.Model(&RMDUser{}).Where("command_to_user IS NOT NULL AND command_to_user <> ''").Count(&pendingCommandCount)
	metrics.PendingCommands.Set(float64(pendingCommandCount))
}
