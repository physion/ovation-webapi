uuids = ["01047b3e-3fc1-4fab-9b3c-bf7f301ea8a5"]
org_id = 247

ActiveRecord::Base.transaction do
  teams = Team.where(uuid: uuids)
  teams.find_each do |team|
    team.entity_id = org_id
    team.save!
  end
end
