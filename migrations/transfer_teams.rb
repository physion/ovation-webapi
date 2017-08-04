uuids = []

puts "Organization id: "
org_id = gets.chomp.to_i

ActiveRecord::Base.transaction do
  teams = Team.where(uuid: uuids)
  teams.find_each do |team|
    puts team.uuid
    team.entity_id = org_id
    team.save!

    # Handle guest org memberships
    team.memberships.each do |membership|
      begin
      membership.send(:ensure_organization_membership)
      rescue
        puts membership.user.email
      end

    end
  end

end
