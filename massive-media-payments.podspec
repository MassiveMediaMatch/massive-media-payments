require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "massive-media-payments"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.description  = <<-DESC
                  massive-media-payments
                   DESC
  s.homepage     = "https://github.com/MassiveMediaMatch/massive-media-payments"
  # brief license entry:
  s.license      = "MIT"
  # optional - use expanded license entry instead:
  # s.license    = { :type => "MIT", :file => "LICENSE" }
  s.authors      = { "Tristan Peeters" => "tristan@massivemedia.eu" }
  s.platforms    = { :ios => "9.0" }
  s.source       = { :git => "https://github.com/MassiveMediaMatch/massive-media-payments.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,c,m,swift}"
  s.requires_arc = true

  s.dependency "React"
  # ...
  # s.dependency "..."
end

